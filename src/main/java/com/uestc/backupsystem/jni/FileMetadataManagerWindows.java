package com.uestc.backupsystem.jni;

import org.springframework.stereotype.Component;

@Component
public class FileMetadataManagerWindows {
    static {
        String fileMetadataManagerWindowsLibPath = "E:\\BackupSystem\\src\\main\\resources\\dll\\file_metadata_manager_windows.dll";
        System.load(fileMetadataManagerWindowsLibPath);
    }

    public native boolean setFileMetadata(String sourcePath, String destinationPath);
}
