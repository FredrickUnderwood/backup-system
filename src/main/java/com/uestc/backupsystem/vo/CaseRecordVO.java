package com.uestc.backupsystem.vo;

import com.uestc.backupsystem.dao.CaseRecordDAO;
import com.uestc.backupsystem.dao.ExecutionRecordDAO;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;

@Getter
@Setter
public class CaseRecordVO {
    private CaseRecordDAO caseRecord;
    private List<ExecutionRecordVO> executionRecordList;
}
