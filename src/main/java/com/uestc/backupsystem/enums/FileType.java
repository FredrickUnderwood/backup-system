package com.uestc.backupsystem.enums;

import lombok.Getter;

@Getter
public enum FileType {
    FILE(0, "FILE"),
    DIRECTORY(1, "DIRECTORY"),
    WINDOWS_SYMBOLIC_LINK(2, "WINDOWS_SYMBOLIC_LINK");

    private final int code;
    private final String message;
    FileType(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
