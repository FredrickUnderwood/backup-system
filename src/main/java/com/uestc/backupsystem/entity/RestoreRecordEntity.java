package com.uestc.backupsystem.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class RestoreRecordEntity {
    private Long id;
    private String sourcePath;
    private String destinationPath;
    private LocalDateTime restoreTime;
    private String status = "SUCCESS";

    public RestoreRecordEntity() {
    }

    @Override
    public String toString() {
        return "RestoreRecordEntity{" +
                "id=" + id +
                ", sourcePath='" + sourcePath + '\'' +
                ", destinationPath='" + destinationPath + '\'' +
                ", restoreTime=" + restoreTime +
                ", status='" + status + '\'' +
                '}';
    }
}
