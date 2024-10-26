package com.uestc.backupsystem.enums;

import lombok.Getter;
import lombok.Setter;
import org.springframework.dao.PermissionDeniedDataAccessException;

@Getter
public enum CreateNewSymbolicLinkStatus {
    SUCCESS(0, "Backup done successfully."),
    PATHS_EXISTED(1, "Path already existed."),
    REQUIRING_ELEVATED_PRIVILEGES(2, "Requiring elevated privileges."),
    OTHERS(3, "Creating symbolic link wrong.");

    private final int code;
    private final String message;

    CreateNewSymbolicLinkStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static String getMessageByCode(int code) {
        for (CreateNewSymbolicLinkStatus status: CreateNewSymbolicLinkStatus.values()) {
            if (status.getCode() == code) {
                return status.getMessage();
            }
        }
        return null;
    }
}
