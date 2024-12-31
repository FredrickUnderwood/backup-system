package com.uestc.backupsystem.dto;

import com.uestc.backupsystem.enums.FileType;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.LinkedHashMap;

@Getter
@Setter
public class SolveDiffResultDTO {
    private boolean isSolveDiffSuccess;
    private LinkedHashMap<File, FileType> solveDiffFailureFileList;
}
