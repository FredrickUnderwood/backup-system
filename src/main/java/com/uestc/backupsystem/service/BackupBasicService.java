package com.uestc.backupsystem.service;

import com.uestc.backupsystem.entity.BackupRecordEntity;

import java.io.IOException;
import java.util.List;

public interface BackupBasicService {
    public void backup(String sourcePath, String destinationPath) throws IOException;

    public void restore(long backupRecordId) throws IOException;

    public List<BackupRecordEntity> getAllBackupRecord();

    public List<BackupRecordEntity> getAllSuccessBackupRecord();
}
