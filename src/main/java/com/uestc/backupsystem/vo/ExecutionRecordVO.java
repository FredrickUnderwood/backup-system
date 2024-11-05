package com.uestc.backupsystem.vo;

import com.uestc.backupsystem.dao.ExecutionRecordDAO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExecutionRecordVO {
    private ExecutionRecordDAO executionRecord;
    private List<FailureFileRecordVO> failureFileRecordList;
}
