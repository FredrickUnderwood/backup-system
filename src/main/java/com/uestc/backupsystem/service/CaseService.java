package com.uestc.backupsystem.service;

import com.uestc.backupsystem.dao.CaseRecordDAO;
import com.uestc.backupsystem.dto.CaseExecutionParamDTO;
import com.uestc.backupsystem.dto.CreateNewCaseParamDTO;
import com.uestc.backupsystem.dto.DeleteCaseParamDTO;
import com.uestc.backupsystem.dto.UpdateCaseBackupPathParamDTO;

public interface CaseService {
    public String createNewCase(CreateNewCaseParamDTO createNewCaseParam);

    public String updateCaseBackupPath(UpdateCaseBackupPathParamDTO updateCaseBackupPathParam);

    public String createNewExecution(CaseExecutionParamDTO caseExecutionParam);

    public String getAllCaseRecords();

    public String getAllExecutionRecordsByCaseId(long caseId);

    public String deleteCase(DeleteCaseParamDTO deleteCaseParam);

}
