package com.uestc.backupsystem.dao;

import com.uestc.backupsystem.enums.FailureType;
import com.uestc.backupsystem.enums.FileType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FailureFileRecordDAO {
    private long id;
    private long executionId;
    private FailureType failureType;
    private String file;
    private FileType fileType;
}
