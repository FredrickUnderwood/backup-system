package com.uestc.backupsystem.enums;

import lombok.Getter;

@Getter
public enum ExecutionType {
    BACKUP(0, "BACKUP"),
    RESTORE(1, "RESTORE");

    private final int code;
    private final String message;

    ExecutionType(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
