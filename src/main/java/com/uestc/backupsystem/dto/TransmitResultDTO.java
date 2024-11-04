package com.uestc.backupsystem.dto;

import com.uestc.backupsystem.enums.FileType;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.LinkedHashMap;

@Setter
@Getter
public class TransmitResultDTO {
    private boolean isTransmitSuccess;
    private LinkedHashMap<File, FileType> transmitFailureFileList;
    private boolean isMetadataSupport;
    private boolean isMetadataSupportSuccess;
    private LinkedHashMap<File, FileType> metadataSupportFailureFileList;
}
