package com.uestc.backupsystem.jni;

import org.springframework.stereotype.Component;

@Component
public class HuffmanCompressionManager {
    static {
        String huffmanCompressionManagerLibPath = "E:\\BackupSystem\\src\\main\\resources\\dll\\huffman_compression_manager.dll";
        System.load(huffmanCompressionManagerLibPath);
    }

    public native void compress(String inputName, String outputName);

    public native void decompress(String inputName, String outputName);
}
