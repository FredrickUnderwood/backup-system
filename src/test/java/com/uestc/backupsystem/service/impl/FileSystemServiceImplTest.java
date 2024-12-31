package com.uestc.backupsystem.service.impl;

import com.uestc.backupsystem.enums.FileType;
import com.uestc.backupsystem.service.FileSystemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FileSystemServiceImplTest {

    @Autowired
    private FileSystemService fileSystemService;
    @Test
    void getFileList() {
    }

    @Test
    void getDiffFileList() {
        File sourceDir = new File("E:\\TestDir_1");
        File destDir = new File("D:\\TestDir_0");
        HashMap<File, FileType> diffFileList = fileSystemService.getDiffFileList(sourceDir, destDir);
        for (File file: diffFileList.keySet()) {
            System.out.println(file.getAbsolutePath());
        }
    }
}