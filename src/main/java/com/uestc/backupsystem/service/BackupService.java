package com.uestc.backupsystem.service;

import com.uestc.backupsystem.dto.ExecutionParamDTO;
import com.uestc.backupsystem.dto.ExecutionResultDTO;

public interface BackupService {
    public ExecutionResultDTO baseBackup(ExecutionParamDTO executionParam);
    public ExecutionResultDTO packBackup(ExecutionParamDTO executionParam);
    public ExecutionResultDTO compressBackup(ExecutionParamDTO executionParam);
}
