package com.uestc.backupsystem.service.impl;

import com.alibaba.fastjson.JSON;
import com.uestc.backupsystem.dao.CaseRecordDAO;
import com.uestc.backupsystem.dao.ExecutionRecordDAO;
import com.uestc.backupsystem.dao.FailureFileRecordDAO;
import com.uestc.backupsystem.dto.*;
import com.uestc.backupsystem.enums.BackupMode;
import com.uestc.backupsystem.mapper.CaseRecordMapper;
import com.uestc.backupsystem.mapper.ExecutionRecordMapper;
import com.uestc.backupsystem.mapper.FailureFileRecordMapper;
import com.uestc.backupsystem.service.BackupService;
import com.uestc.backupsystem.service.CaseService;
import com.uestc.backupsystem.service.RestoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CaseServiceImpl implements CaseService {

    private static final String LOG_PREFIX = "[CaseService]";

    @Autowired
    private CaseRecordMapper caseRecordMapper;

    @Autowired
    private ExecutionRecordMapper executionRecordMapper;

    @Autowired
    private FailureFileRecordMapper failureFileRecordMapper;

    @Autowired
    private BackupService backupService;

    @Autowired
    private RestoreService restoreService;

    @Override
    public String createNewCase(CreateNewCaseParamDTO createNewCaseParam) {
        String sourcePath = createNewCaseParam.getSourcePath();
        String backupPath = createNewCaseParam.getBackupPath();
        sourcePath = sourcePath.replace("\\", "/");
        backupPath = backupPath.replace("\\", "/");
        CreateNewCaseResultDTO createNewCaseResult = new CreateNewCaseResultDTO();
        try {
            List<CaseRecordDAO> caseRecords = caseRecordMapper.getAllCaseRecordsBySourcePathAndBackupPath(sourcePath, backupPath);
            if(!caseRecords.isEmpty()) {
                log.error("{}{} from {} to {}.", LOG_PREFIX, "Case already existed", sourcePath, backupPath);
                createNewCaseResult.setCaseExisted(true);
                return JSON.toJSONString(createNewCaseResult);
            }
            LocalDateTime localDateTimeNow = LocalDateTime.now();
            CaseRecordDAO caseRecord = new CaseRecordDAO();
            caseRecord.setSourcePath(sourcePath);
            caseRecord.setBackupPath(backupPath);
            caseRecord.setCreatedTime(localDateTimeNow);
            caseRecord.setUpdatedTime(localDateTimeNow);
            caseRecordMapper.insertCaseRecord(caseRecord);
            log.info("{}{} from {} to {}.", LOG_PREFIX, "Create new case done", sourcePath, backupPath);
            createNewCaseResult.setCreateNewCaseSuccess(true);
            return JSON.toJSONString(createNewCaseResult);
        } catch (Exception e) {
            log.error("{}{}.", LOG_PREFIX, "Create new case failed", e);
            return JSON.toJSONString(createNewCaseResult);
        }
    }

    @Override
    public String updateCaseBackupPath(UpdateCaseBackupPathParamDTO updateCaseBackupPathParam) {
        long caseId = updateCaseBackupPathParam.getCaseId();
        String newBackupPath = updateCaseBackupPathParam.getNewBackupPath();
        UpdateCaseBackupPathResultDTO updateCaseBackupPathResult = new UpdateCaseBackupPathResultDTO();
        try {
            caseRecordMapper.updateCaseRecordBackupPath(caseId, newBackupPath);
            updateCaseBackupPathResult.setUpdateCaseBackupPathSuccess(true);
            return JSON.toJSONString(updateCaseBackupPathResult);
        } catch (Exception e) {
            log.error("{}{}.", LOG_PREFIX, "Update case backup path failed", e);
            updateCaseBackupPathResult.setUpdateCaseBackupPathSuccess(false);
            return JSON.toJSONString(updateCaseBackupPathResult);
        }

    }

    @Override
    public String createNewExecution(CaseExecutionParamDTO caseExecutionParam) {
        CaseRecordDAO caseRecord = caseRecordMapper.getCaseRecordById(caseExecutionParam.getCaseId());
        ExecutionParamDTO executionParam = new ExecutionParamDTO();
        executionParam.setCaseId(caseExecutionParam.getCaseId());
        executionParam.setSourcePath(caseRecord.getSourcePath());
        executionParam.setDestinationPath(createDestinationPath(caseRecord, caseExecutionParam.getBackupMode()));
        executionParam.setMetadataSupport(caseExecutionParam.isMetadataSupport());
        ExecutionResultDTO executionResult;
        switch (caseExecutionParam.getExecutionType()) {
            case BACKUP -> executionResult = switch (caseExecutionParam.getBackupMode()) {
                case BASE_BACKUP -> backupService.baseBackup(executionParam);
                case PACK_BACKUP -> backupService.packBackup(executionParam);
                case COMPRESS_BACKUP -> backupService.compressBackup(executionParam);
            };
            case RESTORE -> executionResult = switch (caseExecutionParam.getBackupMode()) {
                case BASE_BACKUP -> restoreService.baseRestore(executionParam);
                case PACK_BACKUP -> restoreService.packRestore(executionParam);
                case COMPRESS_BACKUP -> restoreService.compressRestore(executionParam);
            };
            default -> throw new IllegalArgumentException("Unsupported execution type: " + caseExecutionParam.getExecutionType());
        }
        caseRecordMapper.updateCaseRecordUpdatedTime(caseExecutionParam.getCaseId(), LocalDateTime.now());
        return JSON.toJSONString(executionResult);
    }

    @Override
    public String getAllCaseRecords() {
        List<CaseRecordDAO> allCaseRecords = caseRecordMapper.getAllCaseRecords();
        return JSON.toJSONString(allCaseRecords);
    }

    @Override
    public String getAllExecutionRecordsByCaseId(long caseId) {
        List<ExecutionRecordDAO> allExecutionRecordsByCaseId = executionRecordMapper.getAllExecutionRecordsByCaseId(caseId);
        return JSON.toJSONString(allExecutionRecordsByCaseId);
    }

    @Override
    public String deleteCase(DeleteCaseParamDTO deleteCaseParam) {
        long caseId = deleteCaseParam.getCaseId();
        DeleteCaseResultDTO deleteCaseResult = new DeleteCaseResultDTO();
        try {
            caseRecordMapper.deleteCaseById(caseId);
            deleteCaseResult.setDeleteCaseSuccess(true);
            return JSON.toJSONString(deleteCaseResult);
        } catch (Exception e) {
            log.error("{}{}.", LOG_PREFIX, "Delete case failed", e);
            deleteCaseResult.setDeleteCaseSuccess(false);
            return JSON.toJSONString(deleteCaseResult);
        }
    }

    private String createDestinationPath(CaseRecordDAO caseRecord, BackupMode backupMode) {
        String sourcePath = caseRecord.getSourcePath();
        String backupPath = caseRecord.getBackupPath();
        File sourceDir = new File(sourcePath);
        File backupDir = new File(backupPath);
        switch (backupMode) {
            case BASE_BACKUP -> {
                File destinationDir = new File(backupDir, sourceDir.getName());
                return destinationDir.getAbsolutePath();
            }
            case PACK_BACKUP -> {
                String sourceName = sourceDir.getName();
                int dotIndex = sourceName.lastIndexOf('.');
                if (dotIndex > 0) {
                    sourceName = sourceName.substring(0, dotIndex);
                }
                File destinationDir = new File(backupDir, sourceName + ".dat");
                return destinationDir.getAbsolutePath();
            }
            case COMPRESS_BACKUP -> {
                String sourceName = sourceDir.getName();
                int dotIndex = sourceName.lastIndexOf('.');
                if (dotIndex > 0) {
                    sourceName = sourceName.substring(0, dotIndex);
                }
                File destinationDir = new File(backupDir, sourceName + ".bin");
                return destinationDir.getAbsolutePath();
            }
            default -> throw new IllegalArgumentException("Unsupported backup mode: " + backupMode);
        }
    }
}
