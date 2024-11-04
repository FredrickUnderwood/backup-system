package com.uestc.backupsystem.service;

import com.uestc.backupsystem.dto.SolveDiffResultDTO;
import com.uestc.backupsystem.dto.TransmitResultDTO;

public interface SyncService {
    public TransmitResultDTO transmit(String sourcePath, String destinationPath, boolean isMetadataSupport);
    public SolveDiffResultDTO solveDiff(String sourcePath, String destinationPath);
}
