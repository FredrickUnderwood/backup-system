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
#define LOG_PREFIX "[HuffmanCompressionManager]"

struct HuffmanNode
{
    char data;
    int freq;
    std::shared_ptr<HuffmanNode> left, right;
    HuffmanNode(char data, int freq) : data(data), freq(freq), left(nullptr), right(nullptr) {}

    // 重载比较函数，优先队列涉及
    bool operator>(const HuffmanNode &other) const
    {
        return freq > other.freq;
    }
};
struct FileEntry {
    std::string path;
    bool isDirectory;
    uint32_t fileSize;
    FileEntry(const std::string& p, bool isDir, uint32_t size = 0) 
        : path(p), isDirectory(isDir), fileSize(size) {}
};
class HuffmanCompressionManager
{
private:
    // 构建霍夫曼树，返回霍夫曼树的根节点
    static std::shared_ptr<HuffmanNode> buildHuffmanTree(const std::unordered_map<char, int> &freqMap)
    {
        auto cmp = [](std::shared_ptr<HuffmanNode> left, std::shared_ptr<HuffmanNode> right)
        {
            return left->freq > right->freq;
        };
        // 开一个小根堆
        std::priority_queue<std::shared_ptr<HuffmanNode>, std::vector<std::shared_ptr<HuffmanNode>>, decltype(cmp)> minHeap(cmp);
        for (const auto &pair : freqMap)
        {
            minHeap.push(std::make_shared<HuffmanNode>(pair.first, pair.second));
        }
        while (minHeap.size() > 1)
        {
            auto left = minHeap.top();
            minHeap.pop();
            auto right = minHeap.top();
            minHeap.pop();

            auto newHuffmanNode = std::make_shared<HuffmanNode>('\0', left->freq + right->freq);
            newHuffmanNode->left = left;
            newHuffmanNode->right = right;
            minHeap.push(newHuffmanNode);
        }
        return minHeap.top();
    }

    // 文件流输入生成字节频率表
    static std::unordered_map<char, int> buildFreqMap(std::ifstream &file)
    {
        std::unordered_map<char, int> freqMap;
        char data;
        while (file.get(data))
        {
            freqMap[data]++;
        }
        return freqMap;
    }

    // 生成霍夫曼编码
    static void buildHuffmanCodes(const std::shared_ptr<HuffmanNode> &huffmanNode, const std::string &code, std::unordered_map<char, std::string> &huffmanCodes)
    {
        if (!huffmanNode)
            return;
        if (!huffmanNode->left && !huffmanNode->right)
        {
            huffmanCodes[huffmanNode->data] = code;
        }
        buildHuffmanCodes(huffmanNode->left, code + "0", huffmanCodes);
        buildHuffmanCodes(huffmanNode->right, code + "1", huffmanCodes);
    }

    static void compressFile(const std::string inputFileName, const std::string outputFileName)
    {
        std::ifstream inputFile(inputFileName, std::ios::binary);
        if (!inputFile.is_open())
        {
            std::cerr << LOG_PREFIX << "compressFile: Opening input file wrong at " << inputFileName << std::endl;
        }

        auto freqMap = buildFreqMap(inputFile);
        inputFile.clear();
        inputFile.seekg(0, std::ios::beg);

        auto huffmanTreeRootNode = buildHuffmanTree(freqMap);

        std::unordered_map<char, std::string> huffmanCodes;
        buildHuffmanCodes(huffmanTreeRootNode, "", huffmanCodes);

        std::ofstream outputFile(outputFileName, std::ios::binary | std::ios::app);
        if (!outputFile.is_open())
        {
            std::cerr << LOG_PREFIX << "compressFile: Opening output file wrong at " << outputFileName << std::endl;
        }
        // 写入霍夫曼编码表
        outputFile << huffmanCodes.size() << "\n";
        for (const auto &pair : huffmanCodes)
        {
            outputFile << pair.first << ' ' << pair.second << "\n";
        }
        std::string compressedData;
        char data;
        while (inputFile.get(data))
        {
            compressedData += huffmanCodes[data];
        }
        outputFile << compressedData.size() << "\n";
        for (size_t i = 0; i < compressedData.size(); i += 8)
        {
            std::bitset<8> byte(compressedData.substr(i, 8));
            outputFile.put(byte.to_ulong());
        }
        inputFile.close();
        outputFile.close();
    }

    // 重载编码函数，供compressDir使用
    static void compressFile(const std::string inputFileName, std::ofstream &outputFile)
    {
        std::ifstream inputFile(inputFileName, std::ios::binary);
        if (!inputFile.is_open())
        {
            std::cerr << LOG_PREFIX << "compressFile: Opening input file wrong at " << inputFileName << std::endl;
        }

        auto freqMap = buildFreqMap(inputFile);
        inputFile.clear();
        inputFile.seekg(0, std::ios::beg);

        auto huffmanTreeRootNode = buildHuffmanTree(freqMap);

        std::unordered_map<char, std::string> huffmanCodes;
        buildHuffmanCodes(huffmanTreeRootNode, "", huffmanCodes);

        // 写入霍夫曼编码表
        outputFile << huffmanCodes.size() << "\n";
        for (const auto &pair : huffmanCodes)
        {
            outputFile << pair.first << ' ' << pair.second << "\n";
        }
        std::string compressedData;
        char data;
        while (inputFile.get(data))
        {
            compressedData += huffmanCodes[data];
        }
        outputFile << compressedData.size() << "\n";
        for (size_t i = 0; i < compressedData.size(); i += 8)
        {
            std::bitset<8> byte(compressedData.substr(i, 8));
            outputFile.put(byte.to_ulong());
        }
        inputFile.close();
    }

    static void decompressFile(const std::string inputFileName, const std::string outputFileName)
    {
        std::ifstream inputFile(inputFileName, std::ios::binary);
        if (!inputFile.is_open())
        {
            std::cerr << LOG_PREFIX << "decompressFile: Opening input file wrong at " << inputFileName << std::endl;
        }

        // 读取霍夫曼解码表
        std::unordered_map<std::string, char> huffumanCodesDecompress;
        size_t tableSize;
        inputFile >> tableSize;
        inputFile.ignore();

        for (size_t i = 0; i < tableSize; i++)
        {
            char data;
            std::string code;
            inputFile.get(data);
            inputFile >> code;
            inputFile.ignore();
            huffumanCodesDecompress[code] = data;
        }

        size_t compressedDataSize;
        inputFile >> compressedDataSize;
        inputFile.ignore();

        std::string compressedData;
        char byte;
        for (size_t i = 0; i < compressedDataSize; i += 8)
        {
            char byte;
            if (inputFile.get(byte))
            {
                compressedData += std::bitset<8>(byte).to_string();
            }
        }

        // 解码
        std::string decompressedData;
        std::string currentCode;
        for (size_t i = 0; i < compressedDataSize; i++)
        {
            currentCode += compressedData[i];
            if (huffumanCodesDecompress.find(currentCode) != huffumanCodesDecompress.end())
            {
                decompressedData += huffumanCodesDecompress[currentCode];
                currentCode.clear();
            }
        }

        std::ofstream outputFile(outputFileName, std::ios::binary);
        if (!outputFile.is_open())
        {
            std::cerr << LOG_PREFIX << "decompressFile: Opening output file wrong at " << outputFileName << std::endl;
        }
        outputFile << decompressedData;
        inputFile.close();
        outputFile.close();
    }

    // 重载解码函数，供decompressDir使用
    static void decompressFile(std::ifstream &inputFile, const std::string outputFileName)
    {
        // 读取霍夫曼解码表
        std::unordered_map<std::string, char> huffumanCodesDecompress;
        size_t tableSize;
        inputFile >> tableSize;
        inputFile.ignore();

        for (size_t i = 0; i < tableSize; i++)
        {
            char data;
            std::string code;
            inputFile.get(data);
            inputFile >> code;
            inputFile.ignore();
            huffumanCodesDecompress[code] = data;
        }

        size_t compressedDataSize;
        inputFile >> compressedDataSize;
        inputFile.ignore();

        std::string compressedData;
        char byte;
        for (size_t i = 0; i < compressedDataSize; i += 8)
        {
            char byte;
            if (inputFile.get(byte))
            {
                compressedData += std::bitset<8>(byte).to_string();
            }
        }

        // 解码
        std::string decompressedData;
        std::string currentCode;
        for (size_t i = 0; i < compressedDataSize; i++)
        {
            currentCode += compressedData[i];
            if (huffumanCodesDecompress.find(currentCode) != huffumanCodesDecompress.end())
            {
                decompressedData += huffumanCodesDecompress[currentCode];
                currentCode.clear();
            }
        }

        std::ofstream outputFile(outputFileName, std::ios::binary);
        if (!outputFile.is_open())
        {
            std::cerr << LOG_PREFIX << "decompressFile: Opening output file wrong at " << outputFileName << std::endl;
        }
        outputFile << decompressedData;
        outputFile.close();
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

    static void compressDir(const std::string& rootPath, std::ofstream& outputFile) {
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

                // 如果是文件，进行压缩
                if (!current.isDirectory) {
                    compressFile(current.path, outputFile);
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

    static void decompressDir(std::ifstream& inputFile) {
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
                    // 解压文件
                    decompressFile(inputFile, name);
                }
            }
        } catch (const std::exception& e) {
            std::cerr << LOG_PREFIX << "解压失败: " << e.what() << std::endl;
            throw;
        }
    }

public:
    
    static void compress(const std::string& inputName, const std::string& outputName) {
        try {
            DWORD fileAttributes = GetFileAttributes(inputName.c_str());
            if (fileAttributes == INVALID_FILE_ATTRIBUTES) {
                throw std::runtime_error("无法访问输入路径: " + inputName);
            }

            bool isDirectory = (fileAttributes & FILE_ATTRIBUTE_DIRECTORY) != 0;
            if (isDirectory) {
                std::ofstream outputFile(outputName, std::ios::binary);
                if (!outputFile) {
                    throw std::runtime_error("无法创建输出文件: " + outputName);
                }
                compressDir(inputName, outputFile);
                outputFile.close();
            } else {
                compressFile(inputName, outputName);
            }
        } catch (const std::exception& e) {
            std::cerr << LOG_PREFIX << "压缩失败: " << e.what() << std::endl;
            throw;
        }
    }

    static void decompress(const std::string& inputName, const std::string& outputName) {
        try {
            std::ifstream inputFile(inputName, std::ios::binary);
            if (!inputFile) {
                throw std::runtime_error("无法打开输入文件: " + inputName);
            }

            DWORD fileAttributes = GetFileAttributes(outputName.c_str());
            bool outputExists = (fileAttributes != INVALID_FILE_ATTRIBUTES);
            bool isDirectory = outputExists && (fileAttributes & FILE_ATTRIBUTE_DIRECTORY);

            if (outputExists) {
                if (isDirectory) {
                    decompressDir(inputFile);
                } else {
                    decompressFile(inputName, outputName);
                }
            } else {
                // 如果输出路径不存在，先尝试创建目录
                if (!createDirectoryRecursively(outputName)) {
                    throw std::runtime_error("无法创建输出目录: " + outputName);
                }
                decompressDir(inputFile);
            }
            
            inputFile.close();
        } catch (const std::exception& e) {
            std::cerr << LOG_PREFIX << "解压失败: " << e.what() << std::endl;
            throw;
        }
    }
};

extern "C"
{
    JNIEXPORT void JNICALL Java_com_uestc_backupsystem_jni_HuffmanCompressionManager_compressNative(JNIEnv *env, jobject obj, jstring inputName, jstring outputName)
    {
        const char *inputPath = env->GetStringUTFChars(inputName, 0);
        const char *outputPath = env->GetStringUTFChars(outputName, 0);

        HuffmanCompressionManager::compress(inputPath, outputPath);

        env->ReleaseStringUTFChars(inputName, inputPath);
        env->ReleaseStringUTFChars(outputName, outputPath);
    }

    JNIEXPORT void JNICALL Java_com_uestc_backupsystem_jni_HuffmanCompressionManager_decompressNative(JNIEnv *env, jobject obj, jstring inputName, jstring outputName)
    {
        const char *inputPath = env->GetStringUTFChars(inputName, 0);
        const char *outputPath = env->GetStringUTFChars(outputName, 0);

        HuffmanCompressionManager::decompress(inputPath, outputPath);

        env->ReleaseStringUTFChars(inputName, inputPath);
        env->ReleaseStringUTFChars(outputName, outputPath);
    }
}
