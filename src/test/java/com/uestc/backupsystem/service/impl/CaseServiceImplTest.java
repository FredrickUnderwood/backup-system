package com.uestc.backupsystem.service.impl;

import com.uestc.backupsystem.dto.CaseExecutionParamDTO;
import com.uestc.backupsystem.dto.CreateNewCaseParamDTO;
import com.uestc.backupsystem.dto.CreateNewCaseResultDTO;
import com.uestc.backupsystem.enums.BackupMode;
import com.uestc.backupsystem.enums.ExecutionType;
import com.uestc.backupsystem.service.CaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CaseServiceImplTest {

    @Autowired
    private CaseService caseService;

    @Test
    void createNewCase() {
        String sourcePath = "E:/workstation";
        String backupPath = "D:/backup";
        CreateNewCaseParamDTO createNewCaseParam = new CreateNewCaseParamDTO();
        createNewCaseParam.setSourcePath(sourcePath);
        createNewCaseParam.setBackupPath(backupPath);
        caseService.createNewCase(createNewCaseParam);
    }

    @Test
    void updateCaseBackupPath() {
    }

    @Test
    void createNewExecution() {
        long caseId = 1;
        CaseExecutionParamDTO caseExecutionParam = new CaseExecutionParamDTO();
        caseExecutionParam.setBackupMode(BackupMode.BASE_BACKUP);
        caseExecutionParam.setCaseId(caseId);
        caseExecutionParam.setMetadataSupport(true);
        caseExecutionParam.setExecutionType(ExecutionType.BACKUP);
        caseService.createNewExecution(caseExecutionParam);
    }
}