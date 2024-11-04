package com.uestc.backupsystem.service.impl;

import com.uestc.backupsystem.dto.SolveDiffResultDTO;
import com.uestc.backupsystem.dto.TransmitResultDTO;
import com.uestc.backupsystem.enums.FileType;
import com.uestc.backupsystem.jni.FileMetadataManagerWindows;
import com.uestc.backupsystem.jni.SymbolicLinkManagerWindows;
import com.uestc.backupsystem.service.SyncOperationService;
import com.uestc.backupsystem.service.FileSystemService;
import com.uestc.backupsystem.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;

@Slf4j
@Service
public class SyncServiceImpl implements SyncService {

    private static final String LOG_PREFIX = "[SyncService]";

    @Autowired
    private FileSystemService fileSystemService;

    @Autowired
    private SyncOperationService syncOperationService;

    @Autowired
    private FileMetadataManagerWindows fileMetadataManagerWindows;

    @Autowired
    private SymbolicLinkManagerWindows symbolicLinkManagerWindows;


    /**
     *
     * @param sourcePath: E:/workstation
     * @param destinationPath: D:/backup/workstation
     * @return: TransmitResultEntity
     */
    @Override
    public TransmitResultDTO transmit(String sourcePath, String destinationPath, boolean isMetadataSupport) {
        File sourceDir = new File(sourcePath);
        File destinationDir = new File(destinationPath);
        LinkedHashMap<File, FileType> fileList = fileSystemService.getFileList(sourceDir);

        // 构建TransmitResult返回体
        TransmitResultDTO transmitResult = new TransmitResultDTO();
        transmitResult.setMetadataSupport(isMetadataSupport);
        LinkedHashMap<File, FileType> transmitFailureFileList = new LinkedHashMap<>();
        LinkedHashMap<File, FileType> metadataSupportFailureFileList = new LinkedHashMap<>();

        for (Map.Entry<File, FileType> entry: fileList.entrySet()) {
            boolean isTransmitSuccess = true;
            boolean isMetadataSupportSuccess = true;
            File source = entry.getKey();
            FileType sourceType = entry.getValue();

            String relativePath = sourceDir.toPath().relativize(source.toPath()).toString();
            File destination = new File(destinationDir.toPath().resolve(relativePath).toString());

            if (sourceType.equals(FileType.WINDOWS_SYMBOLIC_LINK)) {
                String symbolicLinkTarget = symbolicLinkManagerWindows.getSymbolicLinkTarget(source.getAbsolutePath());
                isTransmitSuccess = syncOperationService.createNewSymbolicLink(symbolicLinkTarget, destination);
            } else if (sourceType.equals(FileType.DIRECTORY)) {
                isTransmitSuccess = syncOperationService.createNewDir(destination);
            } else if (sourceType.equals(FileType.FILE)) {
                isTransmitSuccess = syncOperationService.createNewFile(source, destination);
            }
            if (isMetadataSupport) {
                if (destination.exists() && !destination.canWrite()) {
                    if (!destination.setWritable(true)) {
                        log.error("{}{}: {}", LOG_PREFIX, "File not writable for metadata support", destination);
                    }
                }
                isMetadataSupportSuccess = fileMetadataManagerWindows.setFileMetadata(source.getAbsolutePath(), destination.getAbsolutePath());
                if (!isMetadataSupportSuccess) {
                    metadataSupportFailureFileList.put(source, sourceType);
                    log.error("{}{} for {}.", LOG_PREFIX, "Metadata support failed", source.getAbsolutePath());
                }
            }
            if (!isTransmitSuccess) {
                transmitFailureFileList.put(source, sourceType);
                log.error("{}{} for {}.", LOG_PREFIX, "Transmit failed", source.getAbsolutePath());
            }
        }
        transmitResult.setTransmitSuccess(transmitFailureFileList.isEmpty());
        transmitResult.setTransmitFailureFileList(transmitFailureFileList);
        transmitResult.setMetadataSupportSuccess(metadataSupportFailureFileList.isEmpty());
        transmitResult.setMetadataSupportFailureFileList(metadataSupportFailureFileList);
        log.info("{}{} from {} to {}.", LOG_PREFIX, "Transmit done", sourcePath, destinationPath);
        return transmitResult;
    }

    /**
     *
     * @param sourcePath: E:/workstation
     * @param destinationPath: D:/backup/workstation
     * @return: SolveDiffResultEntity
     */
    @Override
    public SolveDiffResultDTO solveDiff(String sourcePath, String destinationPath) {
        File sourceDir = new File(sourcePath);
        File destinationDir = new File(destinationPath);
        LinkedHashMap<File, FileType> diffFileList = fileSystemService.getDiffFileList(sourceDir, destinationDir);
        ListIterator<Map.Entry<File, FileType>> listIterator = new ArrayList<Map.Entry<File, FileType>>(diffFileList.entrySet()).listIterator(diffFileList.size());
        SolveDiffResultDTO solveDiffResult = new SolveDiffResultDTO();
        LinkedHashMap<File, FileType> solveDiffFailureFileList = new LinkedHashMap<>();
        while(listIterator.hasPrevious()) {
            Map.Entry<File, FileType> entry = listIterator.previous();
            if (!syncOperationService.deleteFile(entry.getKey())) {
                solveDiffFailureFileList.put(entry.getKey(), entry.getValue());
                log.error("{}{} for {}.", LOG_PREFIX, "Delete failed", entry.getKey().getAbsolutePath());

            }
        }
        solveDiffResult.setSolveDiffSuccess(solveDiffFailureFileList.isEmpty());
        solveDiffResult.setSolveDiffFailureFileList(solveDiffFailureFileList);
        log.info("{}{} from {} to {}.", LOG_PREFIX, "Solve diff done", sourcePath, destinationPath);
        return solveDiffResult;
    }
}
