package com.uestc.backupsystem.mapper;

import com.uestc.backupsystem.dao.FailureFileRecordDAO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FailureFileRecordMapper {
    void insertFailureFileRecord(FailureFileRecordDAO failureFileRecord);
}
