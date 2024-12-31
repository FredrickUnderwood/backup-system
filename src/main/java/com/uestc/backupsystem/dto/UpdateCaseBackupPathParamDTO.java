package com.uestc.backupsystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCaseBackupPathParamDTO {
    private long caseId;
    private String newBackupPath;
}
