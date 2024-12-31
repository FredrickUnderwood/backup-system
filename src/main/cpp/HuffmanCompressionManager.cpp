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

    static void compressDir(const std::string inputDirName, std::ofstream &outputFile)
    {
        // 写入当前路径名
        uint32_t dirNameLength = inputDirName.length();
        outputFile.write(reinterpret_cast<const char *>(&dirNameLength), sizeof(dirNameLength));
        outputFile.write(inputDirName.c_str(), dirNameLength);
        // fileSize为0，用于区分目录和文件
        uint32_t fileSize = 0;
        outputFile.write(reinterpret_cast<const char *>(&fileSize), sizeof(fileSize));

        struct _finddata_t file;
        std::string path = inputDirName + "/" + "*";
        long hFile = _findfirst(path.c_str(), &file);
        if (hFile != -1)
        {
            do
            {
                if (file.attrib & _A_SUBDIR)
                {
                    if (strcmp(file.name, ".") != 0 && strcmp(file.name, "..") != 0)
                    {
                        std::string nextPath = inputDirName + "/" + file.name;
                        compressDir(nextPath, outputFile);
                    }
                }
                else
                {
                    std::string filePath = inputDirName + "/" + file.name;

                    // 写入当前文件名
                    uint32_t fileNameLength = filePath.length();
                    outputFile.write(reinterpret_cast<const char *>(&fileNameLength), sizeof(fileNameLength));
                    outputFile.write(filePath.c_str(), fileNameLength);
                    uint32_t fileSize = file.size;
                    outputFile.write(reinterpret_cast<const char *>(&fileSize), sizeof(fileSize));
                    compressFile(filePath, outputFile);
                }
            } while (_findnext(hFile, &file) == 0);
        }
        else
        {
            std::cerr << LOG_PREFIX << "compressDir: Opening dir wrong at " << inputDirName << std::endl;
        }
    }

    static void decompressDir(std::ifstream &inputFile)
    {
        while (!inputFile.eof())
        {
            uint32_t nameLength;
            inputFile.read(reinterpret_cast<char *>(&nameLength), sizeof(nameLength));
            std::string name(nameLength, '\0');
            inputFile.read(&name[0], nameLength);

            uint32_t fileSize;
            inputFile.read(reinterpret_cast<char *>(&fileSize), sizeof(fileSize));

            if (fileSize == 0)
            {
                // 在输出目录中创建相应的目录
                if (CreateDirectory(name.c_str(), NULL) == 0)
                {
                    std::cerr << LOG_PREFIX << "decompressDir: Failed to create directory " << name << std::endl;
                }
            }
            else
            {
                decompressFile(inputFile, name);
            }
        }
    }

public:
    static void compress(const std::string inputName, const std::string outputName)
    {
        DWORD fileAttributes = GetFileAttributes(inputName.c_str());
        bool isDirectory = (fileAttributes & FILE_ATTRIBUTE_DIRECTORY) != 0;
        if (isDirectory)
        {
            std::ofstream outputFile(outputName, std::ios::binary);
            compressDir(inputName, outputFile);
            outputFile.close();
        }
        else
        {
            compressFile(inputName, outputName);
        }
    }

    // backup source
    static void decompress(const std::string inputName, const std::string outputName)
    {
        DWORD fileAttributes = GetFileAttributes(outputName.c_str());
        bool isDirectory = (fileAttributes & FILE_ATTRIBUTE_DIRECTORY) != 0;
        if (isDirectory)
        {
            std::ifstream inputFile(inputName, std::ios::binary);
            decompressDir(inputFile);
            inputFile.close();
        }
        else
        {
            decompressFile(inputName, outputName);
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
