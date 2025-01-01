#include <iostream>
#include <unordered_map>
#include <queue>
#include <vector>
#include <string>
#include <memory>
#include <fstream>
#include <bitset>
#include <stack>
#include <map>
#include <io.h>
#include <windows.h>
#include <filesystem>
#include <jni.h>
#define LOG_PREFIX "[PackManager]"

struct FileEntry {
    std::string path;
    bool isDirectory;
    uint32_t fileSize;
    FileEntry(const std::string& p, bool isDir, uint32_t size = 0) 
        : path(p), isDirectory(isDir), fileSize(size) {}
};
class PackManager
{
private:
    static void packFile(const std::string& inputFileName, std::ofstream& outputFile) {
        std::ifstream inputFile(inputFileName, std::ios::binary);
        if (!inputFile) {
            throw std::runtime_error("无法打开文件: " + inputFileName);
        }

        // 直接复制文件内容
        char buffer[8192];
        while (inputFile.read(buffer, sizeof(buffer))) {
            outputFile.write(buffer, inputFile.gcount());
        }
        if (inputFile.gcount() > 0) {
            outputFile.write(buffer, inputFile.gcount());
        }

        inputFile.close();
    }

    static bool createDirectoryRecursively(const std::string& path) {
        size_t pos = 0;
        do {
            pos = path.find_first_of("/\\", pos + 1);
            std::string subPath = path.substr(0, pos);
            if (!subPath.empty()) {
                if (CreateDirectory(subPath.c_str(), NULL) == 0) {
                    DWORD error = GetLastError();
                    if (error != ERROR_ALREADY_EXISTS) {
                        return false;
                    }
                }
            }
        } while (pos != std::string::npos);
        return true;
    }

    static void packDir(const std::string& rootPath, std::ofstream& outputFile) {
        std::stack<FileEntry> dirStack;
        dirStack.push(FileEntry(rootPath, true));

        while (!dirStack.empty()) {
            FileEntry current = dirStack.top();
            dirStack.pop();

            try {
                // 写入当前路径信息
                uint32_t pathLength = current.path.length();
                outputFile.write(reinterpret_cast<const char*>(&pathLength), sizeof(pathLength));
                outputFile.write(current.path.c_str(), pathLength);
                outputFile.write(reinterpret_cast<const char*>(&current.fileSize), sizeof(current.fileSize));

                // 如果是文件，写入文件内容
                if (!current.isDirectory) {
                    packFile(current.path, outputFile);
                    continue;
                }

                // 处理目录内容
                WIN32_FIND_DATA findData;
                std::string searchPath = current.path + "/*";
                HANDLE hFind = FindFirstFile(searchPath.c_str(), &findData);

                if (hFind != INVALID_HANDLE_VALUE) {
                    do {
                        std::string fileName = findData.cFileName;
                        if (fileName != "." && fileName != "..") {
                            std::string fullPath = current.path + "/" + fileName;
                            bool isDir = (findData.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) != 0;
                            
                            if (isDir) {
                                dirStack.push(FileEntry(fullPath, true, 0));
                            } else {
                                LARGE_INTEGER fileSize;
                                fileSize.LowPart = findData.nFileSizeLow;
                                fileSize.HighPart = findData.nFileSizeHigh;
                                dirStack.push(FileEntry(fullPath, false, 
                                    static_cast<uint32_t>(fileSize.QuadPart)));
                            }
                        }
                    } while (FindNextFile(hFind, &findData));
                    
                    FindClose(hFind);
                } else {
                    throw std::runtime_error("无法访问目录: " + current.path);
                }
            } catch (const std::exception& e) {
                std::cerr << LOG_PREFIX << "Error processing " << current.path 
                         << ": " << e.what() << std::endl;
                throw;
            }
        }
    }

    static void unpackFile(std::ifstream& inputFile, const std::string& outputFileName) {
        try {
            std::ofstream outputFile(outputFileName, std::ios::binary);
            if (!outputFile) {
                throw std::runtime_error("无法创建输出文件: " + outputFileName);
            }

            // 读取文件大小
            uint32_t fileSize;
            if (!inputFile.read(reinterpret_cast<char*>(&fileSize), sizeof(fileSize))) {
                throw std::runtime_error("读取文件大小失败");
            }

            // 复制文件内容
            char buffer[8192];
            uint32_t remainingSize = fileSize;
            while (remainingSize > 0) {
                uint32_t readSize = std::min(remainingSize, (uint32_t)sizeof(buffer));
                if (!inputFile.read(buffer, readSize)) {
                    throw std::runtime_error("读取文件内容失败");
                }
                outputFile.write(buffer, readSize);
                remainingSize -= readSize;
            }

            outputFile.close();
        } catch (const std::exception& e) {
            std::cerr << LOG_PREFIX << "解包文件失败: " << e.what() << std::endl;
            throw;
        }
    }

    static void unpackDir(std::ifstream& inputFile) {
        try {
            while (!inputFile.eof()) {
                // 读取路径信息
                uint32_t nameLength;
                if (!inputFile.read(reinterpret_cast<char*>(&nameLength), sizeof(nameLength))) {
                    if (inputFile.eof()) break;
                    throw std::runtime_error("读取文件长度失败");
                }

                std::string name(nameLength, '\0');
                if (!inputFile.read(&name[0], nameLength)) {
                    throw std::runtime_error("读取文件名失败");
                }

                uint32_t fileSize;
                if (!inputFile.read(reinterpret_cast<char*>(&fileSize), sizeof(fileSize))) {
                    throw std::runtime_error("读取文件大小失败");
                }

                // 创建必要的目录
                size_t lastSlash = name.find_last_of("/\\");
                if (lastSlash != std::string::npos) {
                    std::string dirPath = name.substr(0, lastSlash);
                    if (!dirPath.empty()) {
                        createDirectoryRecursively(dirPath);
                    }
                }

                if (fileSize == 0) {
                    // 创建目录
                    if (!CreateDirectory(name.c_str(), NULL)) {
                        DWORD error = GetLastError();
                        if (error != ERROR_ALREADY_EXISTS) {
                            throw std::runtime_error("创建目录失败: " + name);
                        }
                    }
                } else {
                    // 解包文件
                    std::ofstream outFile(name, std::ios::binary);
                    if (!outFile) {
                        throw std::runtime_error("无法创建文件: " + name);
                    }

                    // 复制文件内容
                    char buffer[8192];
                    uint32_t remainingSize = fileSize;
                    while (remainingSize > 0) {
                        uint32_t readSize = std::min(remainingSize, (uint32_t)sizeof(buffer));
                        if (!inputFile.read(buffer, readSize)) {
                            throw std::runtime_error("读取文件内容失败: " + name);
                        }
                        outFile.write(buffer, readSize);
                        remainingSize -= readSize;
                    }
                    outFile.close();
                }
            }
        } catch (const std::exception& e) {
            std::cerr << LOG_PREFIX << "解包失败: " << e.what() << std::endl;
            throw;
        }
    }

public:
    static void pack(const std::string& inputName, const std::string& outputName) {
        try {
            DWORD fileAttributes = GetFileAttributes(inputName.c_str());
            if (fileAttributes == INVALID_FILE_ATTRIBUTES) {
                throw std::runtime_error("无法访问输入路径: " + inputName);
            }

            std::ofstream outputFile(outputName, std::ios::binary);
            if (!outputFile) {
                throw std::runtime_error("无法创建输出文件: " + outputName);
            }

            bool isDirectory = (fileAttributes & FILE_ATTRIBUTE_DIRECTORY) != 0;
            if (isDirectory) {
                packDir(inputName, outputFile);
            } else {
                // 写入单文件信息
                uint32_t pathLength = inputName.length();
                outputFile.write(reinterpret_cast<const char*>(&pathLength), sizeof(pathLength));
                outputFile.write(inputName.c_str(), pathLength);
                
                LARGE_INTEGER fileSize;
                WIN32_FIND_DATA findData;
                HANDLE hFind = FindFirstFile(inputName.c_str(), &findData);
                if (hFind != INVALID_HANDLE_VALUE) {
                    fileSize.LowPart = findData.nFileSizeLow;
                    fileSize.HighPart = findData.nFileSizeHigh;
                    FindClose(hFind);
                } else {
                    throw std::runtime_error("无法获取文件大小: " + inputName);
                }
                
                uint32_t fileSizeUint = static_cast<uint32_t>(fileSize.QuadPart);
                outputFile.write(reinterpret_cast<const char*>(&fileSizeUint), sizeof(fileSizeUint));
                packFile(inputName, outputFile);
            }
            outputFile.close();
        } catch (const std::exception& e) {
            std::cerr << LOG_PREFIX << "打包失败: " << e.what() << std::endl;
            throw;
        }
    }

    static void unpack(const std::string& inputName, const std::string& outputPath) {
        try {
            std::ifstream inputFile(inputName, std::ios::binary);
            if (!inputFile) {
                throw std::runtime_error("无法打开输入文件: " + inputName);
            }

            DWORD outputAttr = GetFileAttributes(outputPath.c_str());
            bool isOutputDir = (outputAttr != INVALID_FILE_ATTRIBUTES) && 
                             (outputAttr & FILE_ATTRIBUTE_DIRECTORY);

            // 读取第一个条目的路径长度来判断是否是打包的目录
            uint32_t pathLength;
            inputFile.read(reinterpret_cast<char*>(&pathLength), sizeof(pathLength));
            
            // 回到文件开头
            inputFile.seekg(0);

            if (isOutputDir) {
                // 如果输出路径是目录，切换到该目录
                std::string currentDir = std::string(MAX_PATH, '\0');
                GetCurrentDirectory(MAX_PATH, &currentDir[0]);
                SetCurrentDirectory(outputPath.c_str());

                unpackDir(inputFile);

                // 恢复原目录
                SetCurrentDirectory(currentDir.c_str());
            } else {
                // 如果输出路径是文件，直接解包单个文件
                // 跳过路径信息
                std::string path(pathLength, '\0');
                inputFile.read(&path[0], pathLength);
                
                unpackFile(inputFile, outputPath);
            }

            inputFile.close();
        } catch (const std::exception& e) {
            std::cerr << LOG_PREFIX << "解包失败: " << e.what() << std::endl;
            throw;
        }
    }
};

extern "C" {
    JNIEXPORT void JNICALL Java_com_uestc_backupsystem_jni_PackManager_pack(
            JNIEnv *env, jobject obj, jstring inputName, jstring outputName) {
        try {
            const char *inputPath = env->GetStringUTFChars(inputName, 0);
            const char *outputPath = env->GetStringUTFChars(outputName, 0);

            PackManager::pack(inputPath, outputPath);

            env->ReleaseStringUTFChars(inputName, inputPath);
            env->ReleaseStringUTFChars(outputName, outputPath);
        } catch (const std::exception& e) {
            jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
            env->ThrowNew(exceptionClass, e.what());
        }
    }

    JNIEXPORT void JNICALL Java_com_uestc_backupsystem_jni_PackManager_unpack(
            JNIEnv *env, jobject obj, jstring inputName, jstring outputPath) {
        try {
            const char *inputPath = env->GetStringUTFChars(inputName, 0);
            const char *outputDir = env->GetStringUTFChars(outputPath, 0);

            PackManager::unpack(inputPath, outputDir);

            env->ReleaseStringUTFChars(inputName, inputPath);
            env->ReleaseStringUTFChars(outputPath, outputDir);
        } catch (const std::exception& e) {
            jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
            env->ThrowNew(exceptionClass, e.what());
        }
    }
}
