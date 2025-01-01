package com.uestc.backupsystem.jni;

import com.uestc.backupsystem.dto.ComparisonResultDTO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MD5DirectoryComparator {
    static {
        String md5DirectoryComparatorLibPath = null;
        try {
            md5DirectoryComparatorLibPath = new ClassPathResource("dll/md5_directory_comparator.dll").getFile().getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.load(md5DirectoryComparatorLibPath);
    }
    public native ComparisonResultDTO compare(String dir1Path, String dir2Path);
}
