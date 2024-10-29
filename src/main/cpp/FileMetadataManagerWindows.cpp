#include <iostream>
#include <windows.h>
#include <aclapi.h>
#include <jni.h>
#include <nlohmann/json.hpp>
using json = nlohmann::json;

std::string FileTimeToString(const FILETIME& ft) {
    SYSTEMTIME st;
    FileTimeToSystemTime(&ft, &st);
    char buffer[100];
    sprintf(buffer, "%04d-%02d-%02d %02d:%02d:%02d",
            st.wYear, st.wMonth, st.wDay, st.wHour, st.wMinute, st.wSecond);
    return std::string(buffer);
}

FILETIME StringToFileTime(const std::string& str) {
    SYSTEMTIME st = {0};
    FILETIME ft;
    sscanf(str.c_str(), "%04d-%02d-%02d %02d:%02d:%02d",
           &st.wYear, &st.wMonth, &st.wDay, &st.wHour, &st.wMinute, &st.wSecond);
    SystemTimeToFileTime(&st, &ft);
    return ft;
}

class FileMetadataManagerWindows {
    public:
    static std::string getFileMetadata(const std::string& path) {
        json resultlJson;

        // 文件 Mode
        DWORD fileAttributes = GetFileAttributesA(path.c_str());
        if (fileAttributes == INVALID_FILE_ATTRIBUTES) {
            std::cerr << "Getting file attributes wrong from: " << path << ". Wrong: " << GetLastError() << std::endl;
            return "";
        }
        bool isDirectory = (fileAttributes & FILE_ATTRIBUTE_DIRECTORY) != 0;
        bool isArchive = (fileAttributes & FILE_ATTRIBUTE_ARCHIVE) != 0;
        bool isReadOnly = (fileAttributes & FILE_ATTRIBUTE_READONLY) != 0;
        bool isHidden = (fileAttributes & FILE_ATTRIBUTE_HIDDEN) != 0;
        bool isSystem = (fileAttributes & FILE_ATTRIBUTE_SYSTEM) != 0;
        bool isReparsePoint = (fileAttributes & FILE_ATTRIBUTE_REPARSE_POINT) != 0;
        resultlJson["isDirectory"] = isDirectory;
        resultlJson["isArchive"] = isArchive;
        resultlJson["isReadOnly"] = isReadOnly;
        resultlJson["isHidden"] = isHidden;
        resultlJson["isSystem"] = isSystem;
        resultlJson["isReparsePoint"] = isReparsePoint;

        // 句柄
        HANDLE hFile = CreateFileA(
            path.c_str(),
            GENERIC_READ,
            FILE_SHARE_READ,
            NULL,
            OPEN_EXISTING,
            isDirectory ? FILE_FLAG_BACKUP_SEMANTICS : FILE_ATTRIBUTE_NORMAL,
            NULL
        );

        if (hFile == INVALID_HANDLE_VALUE) {
            std::cerr << "Opening file wrong: " << GetLastError() << std::endl;
            CloseHandle(hFile);
            return "";
        }

        // 文件 创建时间，最后访问时间，最后修改时间
        FILETIME creationTime, lastAccessTime, lastWriteTime;
        std::string creationTimeS, lastAccessTimeS, lastWriteTimeS;
        if(GetFileTime(hFile, &creationTime, &lastAccessTime, &lastWriteTime)) {
            creationTimeS = FileTimeToString(creationTime);
            lastAccessTimeS = FileTimeToString(lastAccessTime);
            lastWriteTimeS = FileTimeToString(lastWriteTime);
        } else {
            std::cerr << "Getting file time wrong from: " << path << ". Wrong: " << GetLastError() << std::endl;
            CloseHandle(hFile);
            return "";
        }
        resultlJson["creationTime"] = creationTimeS;
        resultlJson["lastAccessTime"] = lastAccessTimeS;
        resultlJson["lastWriteTime"] = lastWriteTimeS;
        

        // 文件 所有者
        PSID pSidOwner = NULL;
        PSECURITY_DESCRIPTOR pSD = NULL;

        DWORD securityInfo = GetSecurityInfo(
            hFile,
            SE_FILE_OBJECT,
            OWNER_SECURITY_INFORMATION,
            &pSidOwner,
            NULL,
            NULL,
            NULL,
            &pSD
        );

        if (securityInfo != ERROR_SUCCESS) {
            std::cerr << "Getting security info wrong from: " << path << ". Wrong: " << GetLastError() << std::endl;
            CloseHandle(hFile);
            return "";
        }
        char name[256], domain[256];
        DWORD nameSize = sizeof(name);
        DWORD domainSize = sizeof(domain);
        SID_NAME_USE sidType;

        if (!LookupAccountSidA(NULL, pSidOwner, name, &nameSize, domain, &domainSize, &sidType)) {
            std::cerr << "Looking up account sid wrong: " << GetLastError() << std::endl;
            LocalFree(pSD);
            CloseHandle(hFile);
            return "";
        }

        std::string owner = std::string(domain) + "\\" + std::string(name);

        LocalFree(pSD);

        resultlJson["owner"] = owner;

        CloseHandle(hFile);
        return resultlJson.dump();
    }

    static int checkFileMetadata(const std::string sourceFileMetadataJsonS, const std::string destinationFileMetadataJsonS) {
        int result = 0;
        json sourceFileMetadataJson = json::parse(sourceFileMetadataJsonS.c_str());
        json destinationFileMetadataJson = json::parse(destinationFileMetadataJsonS.c_str());
        if (sourceFileMetadataJson["isDirectory"] != destinationFileMetadataJson["isDirectory"] ||
            sourceFileMetadataJson["isArchive"] != destinationFileMetadataJson["isArchive"] ||
            sourceFileMetadataJson["isReadOnly"] != destinationFileMetadataJson["isReadOnly"] ||
            sourceFileMetadataJson["isHidden"] != destinationFileMetadataJson["isHidden"] ||
            sourceFileMetadataJson["isSystem"] != destinationFileMetadataJson["isSystem"] ||
            sourceFileMetadataJson["isReparsePoint"] != destinationFileMetadataJson["isReparsePoint"]) {
            result += 1;
        }
        if (sourceFileMetadataJson["creationTime"] != destinationFileMetadataJson["creationTime"] ||
            sourceFileMetadataJson["lastAccessTime"] != destinationFileMetadataJson["lastAccessTime"] ||
            sourceFileMetadataJson["lastWriteTime"] != destinationFileMetadataJson["lastWriteTime"]) {
            result += 2;
        }
        if (sourceFileMetadataJson["owner"] != destinationFileMetadataJson["owner"]) {
            result += 4;
        }
        return result;
    }

    static bool setFileMetadata(const std::string& sourcePath, const std::string& destinationPath) {
        std::string sourceFileMetadataJsonS = getFileMetadata(sourcePath);
        std::string destinationFileMetadataJsonS = getFileMetadata(destinationPath);
        
        int checkCode = checkFileMetadata(sourceFileMetadataJsonS, destinationFileMetadataJsonS);
        
        json fileMetadataJson = json::parse(sourceFileMetadataJsonS.c_str());

        if (checkCode | 1){
            // 文件 Mode
            DWORD fileAttributes = GetFileAttributesA(destinationPath.c_str());
            if (fileAttributes == INVALID_FILE_ATTRIBUTES) {
                std::cerr << "Getting attributes wrong from: " << destinationPath << ". Wrong: " << GetLastError() << std::endl;
                return false;
            }
            if (fileMetadataJson["isReadOnly"]) {
                fileAttributes |= FILE_ATTRIBUTE_READONLY;
            } else {
                fileAttributes &= ~FILE_ATTRIBUTE_READONLY;
            }
            if (fileMetadataJson["isHidden"]) {
                fileAttributes |= FILE_ATTRIBUTE_HIDDEN;
            } else {
                fileAttributes &= ~FILE_ATTRIBUTE_HIDDEN;
            }
            if (!SetFileAttributesA(destinationPath.c_str(), fileAttributes)) {
                std::cerr << "Setting file attributes wrong for: " << destinationPath << ". Wrong: " << GetLastError() << std::endl;
                return false;
            }
        }
        
        
        if (checkCode | 2) {
            // 文件 创建时间，最后访问时间，最后修改时间
            HANDLE hFileWriteTime = CreateFileA(
                destinationPath.c_str(),
                GENERIC_WRITE,
                0,
                NULL,
                OPEN_EXISTING,
                fileMetadataJson["isDirectory"] ? FILE_FLAG_BACKUP_SEMANTICS : FILE_ATTRIBUTE_NORMAL,
                NULL
            );
            if (hFileWriteTime == INVALID_HANDLE_VALUE) {
                std::cerr << "Opening file wrong: " << GetLastError() << std::endl;
                CloseHandle(hFileWriteTime);
                return false;
            }
            FILETIME creationTime, lastAccessTime, lastWriteTime;
            creationTime = StringToFileTime(fileMetadataJson["creationTime"]);
            lastAccessTime = StringToFileTime(fileMetadataJson["lastAccessTime"]);
            lastWriteTime = StringToFileTime(fileMetadataJson["lastWriteTime"]);

            if (!SetFileTime(hFileWriteTime, &creationTime, &lastAccessTime, &lastWriteTime)) {
                std::cerr << "Setting file time wrong for: " << destinationPath << ". Wrong: " << GetLastError() << std::endl;
                CloseHandle(hFileWriteTime);
                return false;
            }
            CloseHandle(hFileWriteTime);
        }
        
        if (checkCode | 4) {
            // 文件 所有者
            PSID pSidOwner = NULL;
            SID_NAME_USE sidType;
            char domain[256];
            DWORD domainSize = sizeof(domain);
            DWORD sidSize = 0;

            std::string owner = fileMetadataJson["owner"];
            LookupAccountNameA(NULL, owner.c_str(), NULL, &sidSize, domain, &domainSize, &sidType);
            pSidOwner = (PSID)malloc(sidSize);
            if (LookupAccountNameA(NULL, owner.c_str(), pSidOwner, &sidSize, domain, &domainSize, &sidType)) {
                std::cerr << "Looking up account name wrong: " << GetLastError() << std::endl;
                free(pSidOwner);
                return false;
            }

            HANDLE hFileWriteOwner = CreateFileA(
                destinationPath.c_str(),
                WRITE_OWNER,
                0,
                NULL,
                OPEN_EXISTING,
                fileMetadataJson["isDirectory"] ? FILE_FLAG_BACKUP_SEMANTICS : FILE_ATTRIBUTE_NORMAL,
                NULL
            );
            if (hFileWriteOwner == INVALID_HANDLE_VALUE) {
                std::cerr << "Opening file wrong: " << GetLastError() << std::endl;
                CloseHandle(hFileWriteOwner);
                return false;
            }
            if (SetSecurityInfo(hFileWriteOwner, SE_FILE_OBJECT, OWNER_SECURITY_INFORMATION, pSidOwner, NULL, NULL, NULL)) {
                std::cerr << "Setting security info wrong for: " << destinationPath << ". Wrong: " << GetLastError() << std::endl;
                free(pSidOwner);
                CloseHandle(hFileWriteOwner);
                return false;
            }
            free(pSidOwner);
            CloseHandle(hFileWriteOwner);
        }
        return true;
    }
};

extern "C" {
    JNIEXPORT jboolean JNICALL Java_com_uestc_backupsystem_jni_FileMetadataManagerWindows_setFileMetadata(JNIEnv *env, jobject obj, jstring sourcePath, jstring destinationPath) {
        const char *sourcePathS = env->GetStringUTFChars(sourcePath, nullptr);
        const char *destinationPathS = env->GetStringUTFChars(destinationPath, nullptr);
        bool result = FileMetadataManagerWindows::setFileMetadata(sourcePathS, destinationPathS);
        env->ReleaseStringUTFChars(sourcePath, sourcePathS);
        env->ReleaseStringUTFChars(destinationPath, destinationPathS);
        return result;
    }
}