package com.uestc.backupsystem.service;

import java.io.File;

public interface SyncOperationService {
    public boolean createNewSymbolicLink(String symbolicLinkTarget, File symbolicLink);
    public boolean createNewDir(File dir);
    public boolean createNewFile(File sourceFile, File file);
    public boolean deleteFile(File file);
}
