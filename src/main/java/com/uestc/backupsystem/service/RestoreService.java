package com.uestc.backupsystem.service;


import com.uestc.backupsystem.dto.ExecutionResultDTO;

public interface RestoreService {
    public ExecutionResultDTO baseRestore(long caseId);
    public ExecutionResultDTO packRestore(long caseId);
    public ExecutionResultDTO compressRestore(long caseId);
}
