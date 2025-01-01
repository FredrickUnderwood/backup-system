#include <windows.h>
#include <string>
#include <vector>
#include <map>
#include <fstream>
#include <sstream>
#include <iostream>
#include <jni.h>

class DirectoryComparator
{
private:
    static std::string wstringToString(const std::wstring &wstr)
    {
        int len = WideCharToMultiByte(CP_UTF8, 0, wstr.c_str(), -1, NULL, 0, NULL, NULL);
        std::string str(len - 1, 0);
        WideCharToMultiByte(CP_UTF8, 0, wstr.c_str(), -1, &str[0], len, NULL, NULL);
        return str;
    }

    static std::wstring stringToWString(const std::string &str)
    {
        int len = MultiByteToWideChar(CP_UTF8, 0, str.c_str(), -1, NULL, 0);
        std::wstring wstr(len - 1, 0);
        MultiByteToWideChar(CP_UTF8, 0, str.c_str(), -1, &wstr[0], len);
        return wstr;
    }

    // 计算CRC32
    static uint32_t calculateCRC32(const std::string &filePath)
    {
        std::ifstream file(filePath, std::ios::binary);
        if (!file)
        {
            throw std::runtime_error("无法打开文件: " + filePath);
        }

        uint32_t crc = 0xFFFFFFFF;
        char buffer[8192];
        while (file.read(buffer, sizeof(buffer)))
        {
            for (int i = 0; i < file.gcount(); i++)
            {
                crc ^= static_cast<uint8_t>(buffer[i]);
                for (int j = 0; j < 8; j++)
                {
                    crc = (crc >> 1) ^ (0xEDB88320 & -(crc & 1));
                }
            }
        }
        return ~crc;
    }

    // 获取文件大小
    static int64_t getFileSize(const std::string &filePath)
    {
        std::ifstream file(filePath, std::ios::binary | std::ios::ate);
        if (!file)
        {
            throw std::runtime_error("无法打开文件: " + filePath);
        }
        return file.tellg();
    }

    // 获取文件信息（CRC32和大小）
    static std::pair<uint32_t, int64_t> getFileInfo(const std::string& filePath) {
        uint32_t crc = calculateCRC32(filePath);
        int64_t size = getFileSize(filePath);
        return std::make_pair(crc, size);
    }

    // 获取目录下所有文件的相对路径和CRC32值
    static std::map<std::string, std::pair<uint32_t, int64_t>> getDirectoryMap(
        const std::string &dirPath, const std::string &basePath = "")
    {

        std::map<std::string, std::pair<uint32_t, int64_t>> fileMap;
        std::wstring wsearchPath = stringToWString(dirPath + "/*");
        WIN32_FIND_DATAW findData;
        HANDLE hFind = FindFirstFileW(wsearchPath.c_str(), &findData);

        if (hFind == INVALID_HANDLE_VALUE)
        {
            throw std::runtime_error("无法访问目录: " + dirPath);
        }

        do
        {
            std::wstring wFileName = findData.cFileName;
            std::string fileName = wstringToString(wFileName);
            if (fileName != "." && fileName != "..")
            {
                std::string fullPath = dirPath + "/" + fileName;
                std::string relativePath = basePath.empty() ? fileName : basePath + "/" + fileName;

                if (findData.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)
                {
                    auto subDirMap = getDirectoryMap(fullPath, relativePath);
                    fileMap.insert(subDirMap.begin(), subDirMap.end());
                }
                else
                {
                    uint32_t crc = calculateCRC32(fullPath);
                    int64_t size = getFileSize(fullPath);
                    fileMap[relativePath] = std::make_pair(crc, size);
                }
            }
        } while (FindNextFileW(hFind, &findData));

        FindClose(hFind);
        return fileMap;
    }

public:
    struct ComparisonResult
    {
        bool isIdentical;
        std::vector<std::string> differences;
    };

    static ComparisonResult compareDirectories(const std::string& path1, 
                                             const std::string& path2) {
        try {
            ComparisonResult result;
            result.isIdentical = true;

            // 获取路径属性
            DWORD attr1 = GetFileAttributesW(stringToWString(path1).c_str());
            DWORD attr2 = GetFileAttributesW(stringToWString(path2).c_str());

            if (attr1 == INVALID_FILE_ATTRIBUTES || attr2 == INVALID_FILE_ATTRIBUTES) {
                throw std::runtime_error("无法访问路径");
            }

            bool isDir1 = (attr1 & FILE_ATTRIBUTE_DIRECTORY);
            bool isDir2 = (attr2 & FILE_ATTRIBUTE_DIRECTORY);

            // 如果两个都是文件
            if (!isDir1 && !isDir2) {
                auto fileInfo1 = getFileInfo(path1);
                auto fileInfo2 = getFileInfo(path2);

                if (fileInfo1 != fileInfo2) {
                    result.isIdentical = false;
                    result.differences.push_back("文件内容不同");
                }
                return result;
            }

            // 如果一个是文件，一个是目录
            if (isDir1 != isDir2) {
                result.isIdentical = false;
                result.differences.push_back("类型不匹配：一个是文件，一个是目录");
                return result;
            }

            // 如果两个都是目录
            auto fileMap1 = getDirectoryMap(path1);
            auto fileMap2 = getDirectoryMap(path2);

            // 比较目录内容
            for (const auto& pair : fileMap1) {
                auto it = fileMap2.find(pair.first);
                if (it == fileMap2.end()) {
                    result.differences.push_back("文件只存在于目录1: " + pair.first);
                    result.isIdentical = false;
                } else if (it->second != pair.second) {
                    result.differences.push_back("文件内容不同: " + pair.first);
                    result.isIdentical = false;
                }
            }

            for (const auto& pair : fileMap2) {
                if (fileMap1.find(pair.first) == fileMap1.end()) {
                    result.differences.push_back("文件只存在于目录2: " + pair.first);
                    result.isIdentical = false;
                }
            }

            return result;
        } catch (const std::exception& e) {
            throw std::runtime_error("比较失败: " + std::string(e.what()));
        }
    }
};

extern "C"
{
    JNIEXPORT jobject JNICALL Java_com_uestc_backupsystem_jni_MD5DirectoryComparator_compare(JNIEnv *env, jclass cls, jstring dir1Path, jstring dir2Path)
    {
        try
        {
            // 转换 Java 字符串到 C++ 字符串
            const char *dir1 = env->GetStringUTFChars(dir1Path, nullptr);
            const char *dir2 = env->GetStringUTFChars(dir2Path, nullptr);

            // 调用原有的比较函数
            auto result = DirectoryComparator::compareDirectories(dir1, dir2);

            // 释放字符串
            env->ReleaseStringUTFChars(dir1Path, dir1);
            env->ReleaseStringUTFChars(dir2Path, dir2);

            // 创建 Java 对象来存储结果
            jclass resultClass = env->FindClass("com/uestc/backupsystem/dto/ComparisonResultDTO");
            jmethodID constructor = env->GetMethodID(resultClass, "<init>", "()V");
            jobject resultObject = env->NewObject(resultClass, constructor);

            // 设置 isIdentical 字段
            jfieldID isIdenticalField = env->GetFieldID(resultClass, "isIdentical", "Z");
            env->SetBooleanField(resultObject, isIdenticalField, result.isIdentical);

            // 创建差异列表
            jclass arrayListClass = env->FindClass("java/util/ArrayList");
            jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
            jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");

            jobject differencesList = env->NewObject(arrayListClass, arrayListConstructor);

            // 添加差异项
            for (const auto &diff : result.differences)
            {
                jstring diffString = env->NewStringUTF(diff.c_str());
                env->CallBooleanMethod(differencesList, addMethod, diffString);
                env->DeleteLocalRef(diffString);
            }

            // 设置 differences 字段
            jfieldID differencesField = env->GetFieldID(resultClass, "differences", "Ljava/util/List;");
            env->SetObjectField(resultObject, differencesField, differencesList);

            return resultObject;
        }
        catch (const std::exception &e)
        {
            // 抛出 Java 异常
            jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
            env->ThrowNew(exceptionClass, e.what());
            return nullptr;
        }
    }
}