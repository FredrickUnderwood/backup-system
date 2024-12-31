package com.uestc.backupsystem.dao;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
public class CaseRecordDAO {
    private long id;
    private String sourcePath;
    private String backupPath;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
