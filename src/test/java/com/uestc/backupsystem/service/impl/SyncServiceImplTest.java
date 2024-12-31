package com.uestc.backupsystem.service.impl;

import com.uestc.backupsystem.service.SyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SyncServiceImplTest {

    @Autowired
    private SyncService syncService;
    @Test
    void transmit() {
        String sourcePath = "E:/workstation";
        String backupPath = "D:/backup/workstation";
        syncService.transmit(sourcePath, backupPath, true);
    }

    @Test
    void solveDiff() {
        String sourcePath = "E:/workstation";
        String backupPath = "D:/backup/workstation";
        syncService.solveDiff(sourcePath, backupPath);
    }
}