package com.uestc.backupsystem.service.impl;

import com.alibaba.fastjson.JSON;
import com.uestc.backupsystem.dao.CaseRecordDAO;
import com.uestc.backupsystem.dao.ExecutionRecordDAO;
import com.uestc.backupsystem.dao.FailureFileRecordDAO;
import com.uestc.backupsystem.dto.ExecutionParamDTO;
import com.uestc.backupsystem.dto.ExecutionResultDTO;
import com.uestc.backupsystem.dto.SolveDiffResultDTO;
import com.uestc.backupsystem.dto.TransmitResultDTO;
import com.uestc.backupsystem.enums.*;
import com.uestc.backupsystem.mapper.CaseRecordMapper;
import com.uestc.backupsystem.mapper.ExecutionRecordMapper;
import com.uestc.backupsystem.mapper.FailureFileRecordMapper;
import com.uestc.backupsystem.service.RestoreService;
import com.uestc.backupsystem.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class RestoreServiceImpl implements RestoreService {

    private static final String LOG_PREFIX = "[RestoreService]";

    @Autowired
    private SyncService syncService;

    @Autowired
    private ExecutionRecordMapper executionRecordMapper;

    @Autowired
    private FailureFileRecordMapper failureFileRecordMapper;

    @Override
    public ExecutionResultDTO baseRestore(ExecutionParamDTO executionParam) {
        String sourcePath = executionParam.getDestinationPath();
        String destinationPath = executionParam.getSourcePath();
        TransmitResultDTO transmitResult = syncService.transmit(sourcePath, destinationPath, executionParam.isMetadataSupport());
        SolveDiffResultDTO solveDiffResult = syncService.solveDiff(sourcePath, destinationPath);
        log.info("{}{} from {} to {}.", LOG_PREFIX, "Base restore executed", sourcePath, destinationPath);
        // 插入一条执行记录
        ExecutionRecordDAO executionRecord = new ExecutionRecordDAO();
        executionRecord.setCaseId(executionParam.getCaseId());
        executionRecord.setExecutionType(ExecutionType.RESTORE);
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
    public ExecutionResultDTO packRestore(ExecutionParamDTO executionParam) {
        return null;
    }

    @Override
    public ExecutionResultDTO compressRestore(ExecutionParamDTO executionParam) {
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
