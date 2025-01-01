package com.uestc.backupsystem.jni;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FileMetadataManagerWindows {
    static {
        String fileMetadataManagerWindowsLibPath = null;
        try {
            fileMetadataManagerWindowsLibPath = new ClassPathResource("dll/file_metadata_manager_windows.dll").getFile().getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.load(fileMetadataManagerWindowsLibPath);
    }

    public native boolean setFileMetadata(String sourcePath, String destinationPath);
}
