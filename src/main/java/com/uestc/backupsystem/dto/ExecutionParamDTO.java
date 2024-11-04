package com.uestc.backupsystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecutionParamDTO {
    private long caseId;
    private String sourcePath;
    private String destinationPath;
    private boolean isMetadataSupport;
}
