package com.uestc.backupsystem.service.impl;

import com.alibaba.fastjson.JSON;
import com.uestc.backupsystem.dto.ExecutionParamDTO;
import com.uestc.backupsystem.dto.ExecutionResultDTO;
import com.uestc.backupsystem.dto.SolveDiffResultDTO;
import com.uestc.backupsystem.dto.TransmitResultDTO;
import com.uestc.backupsystem.dao.*;
import com.uestc.backupsystem.enums.BackupMode;
import com.uestc.backupsystem.enums.ExecutionType;
import com.uestc.backupsystem.enums.FailureType;
import com.uestc.backupsystem.enums.FileType;
import com.uestc.backupsystem.mapper.ExecutionRecordMapper;
import com.uestc.backupsystem.mapper.FailureFileRecordMapper;
import com.uestc.backupsystem.service.BackupService;
import com.uestc.backupsystem.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;


@Slf4j
@Service
public class BackupServiceImpl implements BackupService {

    private static final String LOG_PREFIX = "[BackupService]";

    @Autowired
    private SyncService syncService;

    @Autowired
    private ExecutionRecordMapper executionRecordMapper;

    @Autowired
    private FailureFileRecordMapper failureFileRecordMapper;

    @Override
    public ExecutionResultDTO baseBackup(ExecutionParamDTO executionParam) {
        String sourcePath = executionParam.getSourcePath();
        String destinationPath = executionParam.getDestinationPath();

        TransmitResultDTO transmitResult = syncService.transmit(sourcePath, destinationPath, executionParam.isMetadataSupport());
        SolveDiffResultDTO solveDiffResult = syncService.solveDiff(sourcePath, destinationPath);
        log.info("{}{} from {} to {}.", LOG_PREFIX, "Base backup executed", sourcePath, destinationPath);

        // 插入一条执行记录
        ExecutionRecordDAO executionRecord = new ExecutionRecordDAO();
        executionRecord.setCaseId(executionParam.getCaseId());
        executionRecord.setExecutionType(ExecutionType.BACKUP);
        executionRecord.setBackupMode(BackupMode.BASE_BACKUP);
        executionRecord.setSourcePath(sourcePath);
        executionRecord.setDestinationPath(destinationPath);
        executionRecord.setExecutionTime(LocalDateTime.now());
        executionRecord.setTransmitSuccess(transmitResult.isTransmitSuccess());
        executionRecord.setSolveDiffSuccess(solveDiffResult.isSolveDiffSuccess());
        executionRecord.setMetadataSupport(transmitResult.isMetadataSupport());
        executionRecord.setMetadataSupportSuccess(transmitResult.isMetadataSupportSuccess());
        executionRecordMapper.insertExecutionRecord(executionRecord);
        log.info("{}{}.", LOG_PREFIX, "Base backup execution_record inserted");

        // 插入失败文件记录
        long executionId = executionRecord.getId();
        insertFailureRecords(executionId, transmitResult.getTransmitFailureFileList(), FailureType.TRANSMIT_FAILURE);
        insertFailureRecords(executionId, transmitResult.getMetadataSupportFailureFileList(), FailureType.METADATA_SUPPORT_FAILURE);
        insertFailureRecords(executionId, solveDiffResult.getSolveDiffFailureFileList(), FailureType.SOLVE_DIFF_FAILURE);


        ExecutionResultDTO executionResult = new ExecutionResultDTO();
        executionResult.setTransmitResultDTO(transmitResult);
        executionResult.setSolveDiffResultDTO(solveDiffResult);
        log.info("{}{}: {}.", LOG_PREFIX, "Base backup result", JSON.toJSONString(executionResult));
        return executionResult;
    }

    @Override
    public ExecutionResultDTO packBackup(ExecutionParamDTO executionParam) {
        return null;
    }

    @Override
    public ExecutionResultDTO compressBackup(ExecutionParamDTO executionParam) {
        return null;
    }

    private void insertFailureRecords(long executionId, LinkedHashMap<File, FileType> failureFileList, FailureType failureType) {
        if(!failureFileList.isEmpty()) {
            for(Map.Entry<File, FileType> entry: failureFileList.entrySet()) {
                FailureFileRecordDAO failureFileRecord = new FailureFileRecordDAO();
                failureFileRecord.setExecutionId(executionId);
                failureFileRecord.setFailureType(failureType);
                failureFileRecord.setFile(entry.getKey().getAbsolutePath());
                failureFileRecord.setFileType(entry.getValue());
                failureFileRecordMapper.insertFailureFileRecord(failureFileRecord);
                log.info("{}{}.", LOG_PREFIX, "Base backup failure_file_record inserted");
            }
        }
    }
}