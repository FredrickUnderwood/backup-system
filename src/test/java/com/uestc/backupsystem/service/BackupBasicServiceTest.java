package com.uestc.backupsystem.service;

import com.uestc.backupsystem.entity.BackupRecordEntity;
import com.uestc.backupsystem.mapper.BackupRecordMapper;
import com.uestc.backupsystem.service.impl.BackupBasicServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BackupBasicServiceTest {

    @Autowired
    private BackupBasicServiceImpl backupBasicService;

    @Autowired
    private BackupRecordMapper backupRecordMapper;

    @Test
    void backup() throws IOException {
        backupBasicService.backup("E:\\LinkSource", "D:\\backup");
    }

    @Test
    void restore() throws IOException {
        BackupRecordEntity backupRecord = backupRecordMapper.getBackupRecordById(2);
        System.out.println(backupRecord.toString());
        backupBasicService.restore(backupRecord.getId());
    }
}