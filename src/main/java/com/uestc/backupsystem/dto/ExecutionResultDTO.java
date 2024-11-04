package com.uestc.backupsystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecutionResultDTO {
    private TransmitResultDTO transmitResultDTO;
    private SolveDiffResultDTO solveDiffResultDTO;
}
