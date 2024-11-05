package com.uestc.backupsystem.mapper;

import com.uestc.backupsystem.dao.FailureFileRecordDAO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FailureFileRecordMapper {
    void insertFailureFileRecord(FailureFileRecordDAO failureFileRecord);
    List<FailureFileRecordDAO> getAllFailureFileRecordsByExecutionId(long executionId);
}
