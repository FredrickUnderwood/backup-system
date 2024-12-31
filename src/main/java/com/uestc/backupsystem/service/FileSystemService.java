package com.uestc.backupsystem.service;

import com.uestc.backupsystem.enums.FileType;

import java.io.File;
import java.util.*;

public interface FileSystemService {
    public LinkedHashMap<File, FileType> getFileList(File dir);
    public LinkedHashMap<File, FileType> getDiffFileList(File sourceDir, File destinationDir);
}
