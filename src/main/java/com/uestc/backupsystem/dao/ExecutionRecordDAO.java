package com.uestc.backupsystem.dao;

import com.uestc.backupsystem.enums.BackupMode;
import com.uestc.backupsystem.enums.ExecutionType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ExecutionRecordDAO {
    private long id;
    private long caseId;
    private ExecutionType executionType;
    private BackupMode backupMode;
    private String sourcePath;
    private String destinationPath;
    private LocalDateTime executionTime;
    private boolean isTransmitSuccess;
    private boolean isSolveDiffSuccess;
    private boolean isMetadataSupport;
    private boolean isMetadataSupportSuccess;
}
