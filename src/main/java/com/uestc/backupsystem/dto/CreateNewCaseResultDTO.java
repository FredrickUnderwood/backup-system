package com.uestc.backupsystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateNewCaseResultDTO {
    private boolean isCaseExisted = false;
    private boolean isCreateNewCaseSuccess = false;
}
