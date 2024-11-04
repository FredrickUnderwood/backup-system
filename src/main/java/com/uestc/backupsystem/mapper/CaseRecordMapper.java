package com.uestc.backupsystem.mapper;

import com.uestc.backupsystem.dao.CaseRecordDAO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CaseRecordMapper {
    CaseRecordDAO getCaseRecordById(long id);
    void insertCaseRecord(CaseRecordDAO caseRecord);
    List<CaseRecordDAO> getAllCaseRecordsBySourcePathAndBackupPath(@Param("sourcePath") String sourcePath, @Param("backupPath") String backupPath);
    void updateCaseRecordUpdatedTime(@Param("id") long id, @Param("updatedTime") LocalDateTime updatedTime);
    void updateCaseRecordBackupPath(@Param("id") long id, @Param("backupPath") String backupPath);
}
