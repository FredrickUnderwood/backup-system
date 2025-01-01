package com.uestc.backupsystem.jni;

import com.uestc.backupsystem.dto.ComparisonResultDTO;
import org.springframework.stereotype.Component;

@Component
public class MD5DirectoryComparator {
    static {
        String md5DirectoryComparatorLibPath = "E:/BackupSystem/src/main/resources/dll/md5_directory_comparator.dll";
        System.load(md5DirectoryComparatorLibPath);
    }
    public native ComparisonResultDTO compare(String dir1Path, String dir2Path);
}
