package com.uestc.backupsystem.service.impl;

import com.uestc.backupsystem.jni.SymbolicLinkManagerWindows;
import com.uestc.backupsystem.service.SyncOperationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;

@Slf4j
@Service
public class SyncOperationServiceImpl implements SyncOperationService {

    private static final String LOG_PREFIX = "[SyncOperationService]";

    private static final Integer BUFFER_SIZE = 8192;

    @Autowired
    private SymbolicLinkManagerWindows symbolicLinkManagerWindows;


    @Override
    public boolean createNewSymbolicLink(String symbolicLinkTarget, File symbolicLink) {
        // 目的符号链接已存在，如果为同名文件或路径，则备份失败；如果为同名符号链接，删除原符号链接，创建新的符号链接
        if (symbolicLink.exists() && !symbolicLink.canWrite()) {
            if (!symbolicLink.setWritable(true)) {
                log.error("{}{}: {}", LOG_PREFIX, "Symbolic link not writable for delete", symbolicLink);
                return false;
            }
        }
        if (symbolicLink.exists()) {
            if (!symbolicLinkManagerWindows.isSymbolicLink(symbolicLink.getAbsolutePath()) || !symbolicLink.delete()) {
                log.error("{}{}: {}", LOG_PREFIX, "Path already existed or it can not be replaced", symbolicLink);
                return false;
            }
        }
        if (symbolicLinkManagerWindows.createNewSymbolicLink(symbolicLinkTarget, symbolicLink.getAbsolutePath())) {
            log.info("{}{}: {}", LOG_PREFIX, "Create new symbolicLink", symbolicLink);
            return true;
        }
        log.error("{}{}: {}", LOG_PREFIX, "Create new symbolicLink failed", symbolicLink);
        return false;
    }

    @Override
    public boolean createNewDir(File dir) {
        // 如果路径存在，且不可写，会影响元数据支持。但是路径是正确生成的，所以如果不可写设置失败，依然返回true
        if (!dir.exists()) {
            if(dir.mkdirs()) {
                log.info("{}{}: {}", LOG_PREFIX, "Create new dir", dir);
                return true;
            }
        } else if(!dir.canWrite() && !dir.setWritable(true)) {
            log.error("{}{}: {}", LOG_PREFIX, "Set dir writable failed", dir);
            log.info("{}{}: {}", LOG_PREFIX, "Dir already existed", dir);
            return true;
        } else {
            log.info("{}{}: {}", LOG_PREFIX, "Dir already existed", dir);
            return true;
        }
        log.error("{}{}: {}", LOG_PREFIX, "Create new dir failed", dir);
        return false;
    }

    @Override
    public boolean createNewFile(File sourceFile, File file) {
        // 如果备份文件夹中的同名文件不可写，则改为可写
        if (file.exists() && !file.canWrite()) {
            if (!file.setWritable(true)) {
                log.error("{}{}: {}", LOG_PREFIX, "File not writable for create", file);
                return false;
            }
        }
        // 在备份文件夹中创建新文件
        try (FileInputStream fis = new FileInputStream(sourceFile); FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int byteRead;
            while ((byteRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, byteRead);
            }
            log.info("{}{}: {}", LOG_PREFIX, "Create new file", file);
            return true;
        } catch (IOException e) {
            log.error("{}{}: {}", LOG_PREFIX, "Create new file failed", file, e);
            return false;
        }
    }

    @Override
    public boolean deleteFile(File file) {
        // 如果备份文件夹中的要删除的文件不可写，则改为可写
        if (file.exists() && !file.canWrite()) {
            if (!file.setWritable(true)) {
                log.error("{}{}: {}", LOG_PREFIX, "File not writable for delete", file);
                return false;
            }
        }
        if (file.delete()) {
            log.info("{}{}: {}", LOG_PREFIX, "Delete file/dir", file);
            return true;
        }
        log.error("{}{}: {}", LOG_PREFIX, "Delete file failed", file);
        return false;
    }
}
