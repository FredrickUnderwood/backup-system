package com.uestc.backupsystem.service;


import com.uestc.backupsystem.dto.ExecutionParamDTO;
import com.uestc.backupsystem.dto.ExecutionResultDTO;

public interface RestoreService {
    public ExecutionResultDTO baseRestore(ExecutionParamDTO executionParam);
    public ExecutionResultDTO packRestore(ExecutionParamDTO executionParam);
    public ExecutionResultDTO compressRestore(ExecutionParamDTO executionParam);
}
