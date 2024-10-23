package com.uestc.backupsystem.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BackupRecordEntity {

    private Long id;
    private String sourcePath;
    private String destinationPath;
    private LocalDateTime backupTime;
    private String status = "SUCCESS";

    public BackupRecordEntity() {
    }

    @Override
    public String toString() {
        return "BackupRecordEntity{" +
                "id=" + id +
                ", sourcePath='" + sourcePath + '\'' +
                ", destinationPath='" + destinationPath + '\'' +
                ", backupTime=" + backupTime +
                ", status='" + status + '\'' +
                '}';
    }
}
