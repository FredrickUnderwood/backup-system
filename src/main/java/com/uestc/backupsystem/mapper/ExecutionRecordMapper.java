package com.uestc.backupsystem.mapper;

import com.uestc.backupsystem.dao.ExecutionRecordDAO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import java.util.List;

@Mapper
public interface ExecutionRecordMapper {
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertExecutionRecord(ExecutionRecordDAO executionRecord);
    List<ExecutionRecordDAO> getAllExecutionRecordsByCaseId(long caseId);
    ExecutionRecordDAO getExecutionRecordById(long id);
}
