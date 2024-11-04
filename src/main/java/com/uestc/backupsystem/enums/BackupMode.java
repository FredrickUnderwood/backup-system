package com.uestc.backupsystem.enums;

import lombok.Getter;

@Getter
public enum BackupMode {
    BASE_BACKUP(0, "BASE_BACKUP"),
    PACK_BACKUP(1, "PACK_BACKUP"),
    COMPRESS_BACKUP(2, "COMPRESS_BACKUP");

    private final int code;
    private final String message;

    BackupMode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
