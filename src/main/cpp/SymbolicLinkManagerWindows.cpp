#include <iostream>
#include <fstream>
#undef _WIN32_WINNT
#define _WIN32_WINNT 0x0600
#include <windows.h>
#include <jni.h>
#define SYMLINK_FLAG_RELATIVE 1

std::wstring jstringToWstring(JNIEnv *env, jstring jstr) {
    const jchar* raw = env->GetStringChars(jstr, nullptr);
    std::wstring result(reinterpret_cast<const wchar_t*>(raw), env->GetStringLength(jstr));
    env->ReleaseStringChars(jstr, raw);
    return result;
}
jstring wstringToJstring(JNIEnv* env, const std::wstring& wstr) {
    // 将 wstring 转换为 UTF-16 编码
    const jchar* unicodeChars = reinterpret_cast<const jchar*>(wstr.c_str());
    return env->NewString(unicodeChars, wstr.length());
}

class SymbolicLinkManagerWindows {
    public:
    static bool isSymbolicLink(const std::wstring& path) {
        // 读取文件
        HANDLE hFile = CreateFileW(
            path.c_str(),
            GENERIC_READ,
            FILE_SHARE_READ | FILE_SHARE_DELETE | FILE_SHARE_WRITE,
            NULL,
            OPEN_EXISTING,
            FILE_FLAG_OPEN_REPARSE_POINT | FILE_FLAG_BACKUP_SEMANTICS, // 打开重解析点本身，而不是目标
            NULL
        );

        if (hFile == INVALID_HANDLE_VALUE) {
            std::cerr << "Opening file wrong: " << GetLastError() << std::endl;
        }

        char buffer[MAXIMUM_REPARSE_DATA_BUFFER_SIZE];
        DWORD dwBytesReturned;

        // 获取解析点信息
        if (!DeviceIoControl(hFile, FSCTL_GET_REPARSE_POINT, NULL, 0, buffer, sizeof(buffer), &dwBytesReturned, NULL)) {
            CloseHandle(hFile);
            return false;
        }

        REPARSE_DATA_BUFFER* reparseData = (REPARSE_DATA_BUFFER*)buffer;
        CloseHandle(hFile);

        if (reparseData->ReparseTag == IO_REPARSE_TAG_SYMLINK) {
            return true;
        }

        return false;
    }

    static std::wstring getSymbolicLinkTarget(const std::wstring& path) {

        HANDLE hFile = CreateFileW(path.c_str(), 
                               GENERIC_READ, 
                               FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE, 
                               NULL, 
                               OPEN_EXISTING, 
                               FILE_FLAG_OPEN_REPARSE_POINT | FILE_FLAG_BACKUP_SEMANTICS, 
                               NULL);

        if (hFile == INVALID_HANDLE_VALUE) {
            std::cerr << "Opening file wrong: " << GetLastError() << std::endl;
        }

        char buffer[MAXIMUM_REPARSE_DATA_BUFFER_SIZE];
        DWORD dwBytesReturned;

        if (!DeviceIoControl(hFile, FSCTL_GET_REPARSE_POINT, NULL, 0, buffer, sizeof(buffer), &dwBytesReturned, NULL)) {
            std::cerr << "Getting reparse point wrong: " << GetLastError() << std::endl;
            CloseHandle(hFile);
        }

        REPARSE_DATA_BUFFER* reparseData = (REPARSE_DATA_BUFFER*)buffer;
        
        wchar_t* targetPath = (wchar_t*)((char*)reparseData->SymbolicLinkReparseBuffer.PathBuffer + reparseData->SymbolicLinkReparseBuffer.SubstituteNameOffset);

        std::wstring result(targetPath, reparseData->SymbolicLinkReparseBuffer.SubstituteNameLength / sizeof(wchar_t));

        // 修正路径，去掉前缀
        const std::wstring prefix = L"\\??\\";
        if (result.compare(0, prefix.size(), prefix) == 0) {
            result.erase(0, prefix.size());
        }

        CloseHandle(hFile);

        DWORD attributes = GetFileAttributesW(path.c_str());
        if (attributes == INVALID_FILE_ATTRIBUTES) {
            std::cerr << "Getting file attributes wrong: " << GetLastError() << std::endl;
        }
        
        bool isDirectory = (attributes & FILE_ATTRIBUTE_DIRECTORY);

        return result;
    }

    private:
    typedef struct _REPARSE_DATA_BUFFER {
    ULONG ReparseTag;
    USHORT ReparseDataLength;
    USHORT Reserved;
    union {
        struct {
            USHORT SubstituteNameOffset;
            USHORT SubstituteNameLength;
            USHORT PrintNameOffset;
            USHORT PrintNameLength;
            ULONG Flags;
            WCHAR PathBuffer[1];
        } SymbolicLinkReparseBuffer;
        struct {
            USHORT SubstituteNameOffset;
            USHORT SubstituteNameLength;
            USHORT PrintNameOffset;
            USHORT PrintNameLength;
            WCHAR PathBuffer[1];
        } MountPointReparseBuffer;
        struct {
            UCHAR DataBuffer[1];
        } GenericReparseBuffer;
        };
    } REPARSE_DATA_BUFFER, *PREPARSE_DATA_BUFFER;
};

extern "C" {
    JNIEXPORT jboolean JNICALL Java_com_uestc_backupsystem_jni_SymbolicLinkManagerWindows_isSymbolicLink(JNIEnv *env, jobject obj, jstring jpath) {
        std::wstring pathW = jstringToWstring(env, jpath);

        bool result = SymbolicLinkManagerWindows::isSymbolicLink(pathW);
        
        return result;
    }

    JNIEXPORT jstring JNICALL Java_com_uestc_backupsystem_jni_SymbolicLinkManagerWindows_getSymbolicLinkTarget(JNIEnv *env, jobject obj, jstring jpath) {
        
        std::wstring pathW = jstringToWstring(env, jpath);

        std::wstring result = SymbolicLinkManagerWindows::getSymbolicLinkTarget(pathW);
        
        return wstringToJstring(env, result);
    }
}
