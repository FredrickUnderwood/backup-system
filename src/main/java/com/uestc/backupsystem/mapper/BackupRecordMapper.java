package com.uestc.backupsystem.mapper;


import com.uestc.backupsystem.entity.BackupRecordEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BackupRecordMapper {
    BackupRecordEntity getBackupRecordById(long id);

    List<BackupRecordEntity> getAllBackupRecord();

    void insertBackupRecord(BackupRecordEntity backupRecordEntity);
}
