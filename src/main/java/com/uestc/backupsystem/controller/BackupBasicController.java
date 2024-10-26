package com.uestc.backupsystem.controller;

import com.uestc.backupsystem.entity.BackupRecordEntity;
import com.uestc.backupsystem.mapper.BackupRecordMapper;
import com.uestc.backupsystem.service.BackupBasicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;


@RestController
public class BackupBasicController {

    @Autowired
    private BackupBasicService backupBasicService;


    @PostMapping("/backup")
    public ResponseEntity<String> executeBackup(@RequestParam String sourcePath, @RequestParam String destinationPath) {
        try {
            backupBasicService.backup(sourcePath, destinationPath);
            return ResponseEntity.ok("Backup is done.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Backup wrong: " + e.getMessage());
        }
    }

    @PostMapping("/restore")
    public ResponseEntity<String> executeRestore(@RequestParam long backupRecordId) {
        try {
            backupBasicService.restore(backupRecordId);
            return ResponseEntity.ok("Restore is done.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Restore wrong: " + e.getMessage());
        }
    }

    @GetMapping("/")
    public List<BackupRecordEntity> getAllSuccessBackupRecord() {
        return backupBasicService.getAllSuccessBackupRecord();
    }
}
