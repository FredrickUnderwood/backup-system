package com.uestc.backupsystem.mapper;


import com.uestc.backupsystem.entity.RestoreRecordEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RestoreRecordMapper {
    RestoreRecordEntity getRestoreRecordById(long id);

    List<RestoreRecordEntity> getAllRestoreRecord();

    void insertRestoreRecord(RestoreRecordEntity restoreRecordEntity);
}
