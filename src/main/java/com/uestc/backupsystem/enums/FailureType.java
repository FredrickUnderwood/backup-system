package com.uestc.backupsystem.enums;

public enum FailureType {
    TRANSMIT_FAILURE(0, "TRANSMIT_FAILURE"),
    METADATA_SUPPORT_FAILURE(1, "METADATA_SUPPORT_FAILURE"),
    SOLVE_DIFF_FAILURE(2, "SOLVE_DIFF_FAILURE");

    private final int code;
    private final String message;
    FailureType(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
