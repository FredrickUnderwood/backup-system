package com.uestc.backupsystem.dto;

import com.uestc.backupsystem.enums.BackupMode;
import com.uestc.backupsystem.enums.ExecutionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaseExecutionParamDTO {
    private long caseId;
    private ExecutionType executionType;
    private BackupMode backupMode;
    private boolean isMetadataSupport;
}
