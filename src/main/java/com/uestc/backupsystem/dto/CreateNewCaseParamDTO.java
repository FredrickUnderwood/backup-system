package com.uestc.backupsystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateNewCaseParamDTO {
    private String sourcePath;
    private String backupPath;
}
