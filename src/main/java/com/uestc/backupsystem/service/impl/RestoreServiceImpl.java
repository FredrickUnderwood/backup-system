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
import com.uestc.backupsystem.jni.HuffmanCompressionManager;
import com.uestc.backupsystem.jni.PackManager;
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

    @Autowired
    private HuffmanCompressionManager huffmanCompressionManager;

    @Autowired
    private PackManager packManager;

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
        log.info("{}{}.", LOG_PREFIX, "Base restore execution_record inserted");

        // 插入失败文件记录
        long executionId = executionRecord.getId();
        insertFailureRecords(executionId, transmitResult.getTransmitFailureFileList(), FailureType.TRANSMIT_FAILURE);
        insertFailureRecords(executionId, transmitResult.getMetadataSupportFailureFileList(), FailureType.METADATA_SUPPORT_FAILURE);
        insertFailureRecords(executionId, solveDiffResult.getSolveDiffFailureFileList(), FailureType.SOLVE_DIFF_FAILURE);


        ExecutionResultDTO executionResult = new ExecutionResultDTO();
        executionResult.setTransmitResultDTO(transmitResult);
        executionResult.setSolveDiffResultDTO(solveDiffResult);
        log.info("{}{}: {}.", LOG_PREFIX, "Base restore result", JSON.toJSONString(executionResult));
        return executionResult;
    }

    @Override
    public ExecutionResultDTO packRestore(ExecutionParamDTO executionParam) {
        String sourcePath = executionParam.getDestinationPath(); // D:/backup/workstation.dat
        String destinationPath = executionParam.getSourcePath(); // E:/workstation
        String destinationFilePath = new File(destinationPath).getParentFile().getAbsolutePath(); // E:/
        // 执行备份
        packManager.unpack(sourcePath, destinationFilePath);
        log.info("{}{} from {} to {}.", LOG_PREFIX, "Pack restore executed", sourcePath, destinationPath);

        // 插入一条执行记录
        ExecutionRecordDAO executionRecord = new ExecutionRecordDAO();
        executionRecord.setCaseId(executionParam.getCaseId());
        executionRecord.setExecutionType(ExecutionType.RESTORE);
        executionRecord.setBackupMode(BackupMode.PACK_BACKUP);
        executionRecord.setSourcePath(sourcePath);
        executionRecord.setDestinationPath(destinationPath);
        executionRecord.setExecutionTime(LocalDateTime.now());
        executionRecordMapper.insertExecutionRecord(executionRecord);
        log.info("{}{}.", LOG_PREFIX, "Pack restore execution_record inserted");

        TransmitResultDTO transmitResult = new TransmitResultDTO();
        transmitResult.setTransmitSuccess(true);
        SolveDiffResultDTO solveDiffResult = new SolveDiffResultDTO();
        solveDiffResult.setSolveDiffSuccess(true);
        ExecutionResultDTO executionResult = new ExecutionResultDTO();
        executionResult.setTransmitResultDTO(transmitResult);
        executionResult.setSolveDiffResultDTO(solveDiffResult);
        log.info("{}{}: {}.", LOG_PREFIX, "Pack restore result", JSON.toJSONString(executionResult));
        return executionResult;
    }

    @Override
    public ExecutionResultDTO compressRestore(ExecutionParamDTO executionParam) {
        String sourcePath = executionParam.getDestinationPath(); // D:/backup/workstation.bin
        String destinationPath = executionParam.getSourcePath(); // E:/workstation
        String formatDestinationPath = destinationPath.replace("\\", "/");
        // 执行备份
        huffmanCompressionManager.decompress(sourcePath, formatDestinationPath);
        log.info("{}{} from {} to {}.", LOG_PREFIX, "Compress restore executed", sourcePath, destinationPath);

        // 插入一条执行记录
        ExecutionRecordDAO executionRecord = new ExecutionRecordDAO();
        executionRecord.setCaseId(executionParam.getCaseId());
        executionRecord.setExecutionType(ExecutionType.RESTORE);
        executionRecord.setBackupMode(BackupMode.COMPRESS_BACKUP);
        executionRecord.setSourcePath(sourcePath);
        executionRecord.setDestinationPath(destinationPath);
        executionRecord.setExecutionTime(LocalDateTime.now());
        executionRecordMapper.insertExecutionRecord(executionRecord);
        log.info("{}{}.", LOG_PREFIX, "Compress restore execution_record inserted");

        TransmitResultDTO transmitResult = new TransmitResultDTO();
        transmitResult.setTransmitSuccess(true);
        SolveDiffResultDTO solveDiffResult = new SolveDiffResultDTO();
        solveDiffResult.setSolveDiffSuccess(true);
        ExecutionResultDTO executionResult = new ExecutionResultDTO();
        executionResult.setTransmitResultDTO(transmitResult);
        executionResult.setSolveDiffResultDTO(solveDiffResult);
        log.info("{}{}: {}.", LOG_PREFIX, "Compress restore result", JSON.toJSONString(executionResult));
        return executionResult;
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
