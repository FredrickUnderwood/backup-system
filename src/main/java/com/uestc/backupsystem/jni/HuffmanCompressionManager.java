package com.uestc.backupsystem.jni;

import org.springframework.stereotype.Component;

@Component
public class HuffmanCompressionManager {
    static {
        String huffmanCompressionManagerLibPath = "E:/BackupSystem/src/main/resources/dll/huffman_compression_manager.dll";
        System.load(huffmanCompressionManagerLibPath);
    }

    // 本地方法声明
    private native void compressNative(String inputName, String outputName);
    private native void decompressNative(String inputName, String outputName);

    // 对外公开的压缩方法
    public void compress(String inputName, String outputName) {
        try {
            compressNative(inputName, outputName);
        } catch (Exception e) {
            throw new RuntimeException("Compression failed", e);
        }
    }

    // 对外公开的解压方法
    public void decompress(String inputName, String outputName) {
        try {
            decompressNative(inputName, outputName);
        } catch (Exception e) {
            throw new RuntimeException("Decompression failed", e);
        }
    }
}
