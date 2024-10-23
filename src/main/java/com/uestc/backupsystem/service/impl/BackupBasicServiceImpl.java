package com.uestc.backupsystem.service.impl;

import com.uestc.backupsystem.entity.BackupRecordEntity;
import com.uestc.backupsystem.entity.RestoreRecordEntity;
import com.uestc.backupsystem.mapper.BackupRecordMapper;
import com.uestc.backupsystem.mapper.RestoreRecordMapper;
import com.uestc.backupsystem.service.BackupBasicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Stack;


@Slf4j
@Service
public class BackupBasicServiceImpl implements BackupBasicService {

    @Autowired
    private BackupRecordMapper backupRecordMapper;

    @Autowired
    private RestoreRecordMapper restoreRecordMapper;

    private static final String SUCCESS_STATUS = "SUCCESS";

    private static final Integer BUFFER_SIZE = 8192;


    @Override
    public void backup(String sourcePath, String destinationPath) throws IOException {
        File source = new File(sourcePath);
        File destination = new File(destinationPath);

        if (!source.exists()) {
            throw new FileNotFoundException("Source directory not found: " + source.getAbsolutePath());
        }


        if (source.isFile()) {
            // 处理文件
            File destinationFile = new File(destinationPath, source.getName());
            destinationPath = destinationFile.getAbsolutePath();
            executeFileTransfer(source, destinationFile);
        } else if (source.isDirectory()) {
            // 处理目录
            File destinationDir = new File(destination, source.getName());
            destinationPath = destinationDir.getAbsolutePath();
            executeDirTransfer(source, destinationDir);
        } else {
            throw new IOException("Source directory wrong: " + source.getAbsolutePath());
        }
        BackupRecordEntity backupRecord = new BackupRecordEntity();
        backupRecord.setSourcePath(sourcePath);
        backupRecord.setDestinationPath(destinationPath);
        backupRecord.setBackupTime(LocalDateTime.now());
        backupRecord.setStatus(SUCCESS_STATUS);
        backupRecordMapper.insertBackupRecord(backupRecord);
        log.info("Backup execution info: " + backupRecord.toString());
    }

    @Override
    public void restore(long backupRecordId) throws IOException {
        BackupRecordEntity backupRecord = backupRecordMapper.getBackupRecordById(backupRecordId);
        String sourcePath = backupRecord.getSourcePath();
        String destinationPath = backupRecord.getDestinationPath();

        File source = new File(sourcePath);
        File destination = new File(destinationPath);

        if (destination.isFile()) {
            executeFileTransfer(destination, source);
        } else if (destination.isDirectory()) {
            executeDirTransfer(destination, source);
        } else {
            throw new IOException("Source directory wrong: " + source.getAbsolutePath());
        }

        RestoreRecordEntity restoreRecord = new RestoreRecordEntity();
        restoreRecord.setSourcePath(sourcePath);
        restoreRecord.setDestinationPath(destinationPath);
        restoreRecord.setRestoreTime(LocalDateTime.now());
        restoreRecord.setStatus(SUCCESS_STATUS);
        restoreRecordMapper.insertRestoreRecord(restoreRecord);
        log.info("Backup execution info: " + restoreRecord.toString());
    }

    @Override
    public List<BackupRecordEntity> getAllBackupRecord() {
        return backupRecordMapper.getAllBackupRecord();
    }

    private void executeFileTransfer(File sourceFile, File destinationFile) throws IOException {
        if (!sourceFile.exists()) {
            throw new FileNotFoundException("Source file not found: " + sourceFile.getAbsolutePath());
        }
        if (!sourceFile.isFile()) {
            throw new IOException("Source path is not a file: " + sourceFile.getAbsolutePath());
        }
        File parentDir = destinationFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        try (FileInputStream fis = new FileInputStream(sourceFile); FileOutputStream fos = new FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int byteRead;
            while ((byteRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, byteRead);
            }
        }

    }

    private void executeDirTransfer(File sourceDir, File destinationDir) throws IOException {
        Stack<File[]> stack = new Stack<>();
        stack.push(new File[]{sourceDir, destinationDir});
        while (!stack.empty()) {
            File[] current = stack.pop();
            File currentSourceDir = current[0];
            File currentDestinationDir = current[1];
            if (!currentDestinationDir.exists()) {
                currentDestinationDir.mkdirs();
            }
            File[] files = currentSourceDir.listFiles();
            if (files != null) {
                for (File file: files) {
                    File destinationFile = new File(currentDestinationDir, file.getName());
                    if (file.isDirectory()) {
                        stack.push(new File[]{file, destinationFile});
                    } else if (file.isFile()) {
                        executeFileTransfer(file, destinationFile);
                    }
                }
            }
        }
    }
}
