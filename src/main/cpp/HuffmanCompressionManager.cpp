#include <iostream>
#include <unordered_map>
#include <queue>
#include <vector>
#include <string>
#include <memory>
#include <fstream>
#include <bitset>
#define LOG_PREFIX "[HuffmanCompressionManager]"

struct HuffmanNode {
    char data;
    int freq;
    std::shared_ptr<HuffmanNode> left, right;
    HuffmanNode(char data, int freq) : data(data), freq(freq), left(nullptr), right(nullptr) {}

    // 重载比较函数，优先队列涉及
    bool operator>(const HuffmanNode &other) const {
        return freq > other.freq;
    }
};
class HuffmanCompressionManager {
    private:
    // 构建霍夫曼树，返回霍夫曼树的根节点
    static std::shared_ptr<HuffmanNode> buildHuffmanTree(const std::unordered_map<char, int>& freqMap) {
        auto cmp = [](std::shared_ptr<HuffmanNode> left, std::shared_ptr<HuffmanNode> right) {
            return left->freq > right->freq;
        };
        // 开一个小根堆
        std::priority_queue<std::shared_ptr<HuffmanNode>, std::vector<std::shared_ptr<HuffmanNode>>, decltype(cmp)> minHeap(cmp);
        for (const auto& pair: freqMap) {
            minHeap.push(std::make_shared<HuffmanNode>(pair.first, pair.second));
        }
        while (minHeap.size() > 1) {
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
    static std::unordered_map<char, int> buildFreqMap(std::ifstream& file) {
        std::unordered_map<char, int> freqMap;
        char data;
        while (file.get(data)) {
            freqMap[data]++;
        }
        return freqMap;
    }

    // 生成霍夫曼编码
    static void buildHuffmanCodes(const std::shared_ptr<HuffmanNode>& huffmanNode, const std::string& code, std::unordered_map<char, std::string>& huffmanCodes) {
        if (!huffmanNode) return;
        if (!huffmanNode->left && !huffmanNode->right) {
            huffmanCodes[huffmanNode->data] = code;
        }
        buildHuffmanCodes(huffmanNode->left, code + "0", huffmanCodes);
        buildHuffmanCodes(huffmanNode->right, code + "1", huffmanCodes);
    }

    public:
    static void compressFile(const std::string inputFileName, const std::string outputFileName) {
        std::ifstream inputFile(inputFileName, std::ios::binary);
        if (!inputFile.is_open()) {
            std::cerr << LOG_PREFIX << "compressFile: Opening input file wrong at " << inputFileName << std::endl;
        }

        auto freqMap = buildFreqMap(inputFile);
        inputFile.clear();
        inputFile.seekg(0, std::ios::beg);

        auto huffmanTreeRootNode = buildHuffmanTree(freqMap);

        std::unordered_map<char, std::string> huffmanCodes;
        buildHuffmanCodes(huffmanTreeRootNode, "", huffmanCodes);

        std::ofstream outputFile(outputFileName, std::ios::binary);
        if (!outputFile.is_open()) {
            std::cerr << LOG_PREFIX << "compressFile: Opening output file wrong at " << outputFileName << std::endl;
        }
        // 写入霍夫曼编码表
        outputFile << huffmanCodes.size() << "\n";
        for (const auto& pair: huffmanCodes) {
            outputFile << pair.first << ' ' << pair.second << "\n";
        }
        std::string compressedData;
        char data;
        while (inputFile.get(data)) {
            compressedData += huffmanCodes[data];
        }
        outputFile << compressedData.size() << "\n";
        for (size_t i = 0; i < compressedData.size() ; i += 8) {
            std::bitset<8> byte(compressedData.substr(i, 8));
            outputFile.put(byte.to_ulong());
        }
        inputFile.close();
        outputFile.close();
    }
    static void decompressFile(const std::string inputFileName, const std::string outputFileName) {
        std::ifstream inputFile(inputFileName, std::ios::binary);
        if (!inputFile.is_open()) {
            std::cerr << LOG_PREFIX << "decompressFile: Opening input file wrong at " << inputFileName << std::endl;
        }

        // 读取霍夫曼解码表
        std::unordered_map<std::string, char> huffumanCodesDecompress;
        size_t tableSize;
        inputFile >> tableSize;
        inputFile.ignore();

        for (size_t i = 0; i < tableSize; i ++) {
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
        while (inputFile.get(byte)) {
            compressedData += std::bitset<8>(byte).to_string();
        }

        // 解码
        std::string decompressedData;
        std::string currentCode;
        for (size_t i = 0; i < compressedDataSize; i++) {
            currentCode += compressedData[i];
            if (huffumanCodesDecompress.find(currentCode) != huffumanCodesDecompress.end()) {
                decompressedData += huffumanCodesDecompress[currentCode];
                currentCode.clear();
            }
        }

        std::ofstream outputFile(outputFileName, std::ios::binary);
        if (!outputFile.is_open()) {
            std::cerr << LOG_PREFIX << "decompressFile: Opening output file wrong at " << outputFileName << std::endl;
        }
        outputFile << decompressedData;
        inputFile.close();
        outputFile.close();
    }
};