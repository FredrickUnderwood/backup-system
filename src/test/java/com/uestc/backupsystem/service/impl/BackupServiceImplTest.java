package com.uestc.backupsystem.service.impl;

import com.uestc.backupsystem.service.BackupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class BackupServiceImplTest {

    @Autowired
    private BackupService backupService;

    @Test
    void baseBackup() {
        String sourcePath = "E:/workstation";
        String backupPath = "D:/backup";

    }

    @Test
    void packBackup() {
    }

    @Test
    void compressBackup() {
    }
}