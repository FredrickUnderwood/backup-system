package com.uestc.backupsystem.service.impl;

import com.uestc.backupsystem.dto.ExecutionResultDTO;
import com.uestc.backupsystem.service.RestoreService;
import org.springframework.stereotype.Service;

@Service
public class RestoreServiceImpl implements RestoreService {
    @Override
    public ExecutionResultDTO baseRestore(long caseId) {
        return null;
    }

    @Override
    public ExecutionResultDTO packRestore(long caseId) {
        return null;
    }

    @Override
    public ExecutionResultDTO compressRestore(long caseId) {
        return null;
    }
}
