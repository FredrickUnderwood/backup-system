package com.uestc.backupsystem.service.impl;

import com.uestc.backupsystem.enums.FileType;
import com.uestc.backupsystem.jni.SymbolicLinkManagerWindows;
import com.uestc.backupsystem.service.FileSystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Stack;

@Slf4j
@Service
public class FileSystemServiceImpl implements FileSystemService {

    private static final String LOG_PREFIX = "[FileSystemService]";

    @Autowired
    private SymbolicLinkManagerWindows symbolicLinkManagerWindows;

    @Override
    public LinkedHashMap<File, FileType> getFileList(File dir) {
        LinkedHashMap<File, FileType> fileList = new LinkedHashMap<>();

        // 传入内容不为目录，直接处理
        if (!dir.isDirectory()) {
            if (symbolicLinkManagerWindows.isSymbolicLink(dir.getAbsolutePath())) {
                fileList.put(dir, FileType.WINDOWS_SYMBOLIC_LINK);
            } else if (dir.isFile()) {
                fileList.put(dir, FileType.FILE);
            }
            log.info("{}{} for {}.", LOG_PREFIX, "File tree constructed", dir.getAbsolutePath());
            return fileList;
        }

        Stack<File> dirStack = new Stack<>();
        dirStack.push(dir);
        while (!dirStack.isEmpty()) {
            File currentDir = dirStack.pop();
            fileList.put(currentDir, FileType.DIRECTORY);

            File[] currentDirFiles = currentDir.listFiles();
            if (currentDirFiles == null) {
                continue;
            }
            for (File currentDirFile: currentDirFiles) {
                if (symbolicLinkManagerWindows.isSymbolicLink(currentDirFile.getAbsolutePath())) {
                    fileList.put(currentDirFile, FileType.WINDOWS_SYMBOLIC_LINK);
                } else if (currentDirFile.isFile()) {
                    fileList.put(currentDirFile, FileType.FILE);
                } else if (currentDirFile.isDirectory()) {
                    fileList.put(currentDirFile, FileType.DIRECTORY);
                    dirStack.push(currentDirFile);
                }
            }
        }
        log.info("{}{} for {}.", LOG_PREFIX, "File tree constructed", dir.getAbsolutePath());
        return fileList;
    }

    /**
     *
     * @param sourceDir: E:/workstation
     * @param destinationDir: D:/backup/workstation
     * @return: LinkedHashMap<File, FileType> diffFileList
     */
    @Override
    public LinkedHashMap<File, FileType> getDiffFileList(File sourceDir, File destinationDir) {
        // 传入内容不为目录，直接处理
        LinkedHashMap<File, FileType> diffFileList = new LinkedHashMap<>();
        if (!sourceDir.isDirectory()) {
            return diffFileList;
        }
        LinkedHashMap<File, FileType> sourceDirFileList = getFileList(sourceDir);
        LinkedHashMap<File, FileType> destinationDirFileList = getFileList(destinationDir);
        for (File file: destinationDirFileList.keySet()) {
            String relativePath = destinationDir.toPath().relativize(file.toPath()).toString();
            File sourceFileEquivalent = new File(sourceDir, relativePath);
            if (!sourceDirFileList.containsKey(sourceFileEquivalent)) {
                diffFileList.put(file, destinationDirFileList.get(file));
            }
        }
        log.info("{}{} for {} and {}.", LOG_PREFIX, "Diff file list constructed", sourceDir.getAbsolutePath(), destinationDir.getAbsolutePath());
        return diffFileList;
    }
}
