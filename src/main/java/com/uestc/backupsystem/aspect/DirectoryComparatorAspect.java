package com.uestc.backupsystem.aspect;

import com.uestc.backupsystem.dto.ComparisonResultDTO;
import com.uestc.backupsystem.dto.ExecutionParamDTO;
import com.uestc.backupsystem.dto.ExecutionResultDTO;
import com.uestc.backupsystem.enums.FileType;
import com.uestc.backupsystem.jni.MD5DirectoryComparator;
import com.uestc.backupsystem.mapper.ExecutionRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.LinkedHashMap;

@Slf4j
@Component
@Aspect
public class DirectoryComparatorAspect {

    private static final String LOG_PREFIX = "[DirectoryComparator]";
    @Autowired
    private MD5DirectoryComparator md5DirectoryComparator;

    @Autowired
    private ExecutionRecordMapper executionRecordMapper;


    @Pointcut("execution(* com.uestc.backupsystem.service.impl.BackupServiceImpl.base*(..)) || execution(* com.uestc.backupsystem.service.impl.RestoreServiceImpl.base*(..))")
    public void directoryComparatorPointCut() {}

    @Around(value = "directoryComparatorPointCut()")
    public Object directoryCompare(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        // 获取当前返回值
        ExecutionResultDTO executionResultDTO = (ExecutionResultDTO) proceedingJoinPoint.proceed();

        Object[] args = proceedingJoinPoint.getArgs();
        ExecutionParamDTO executionParamDTO = (ExecutionParamDTO) args[0];
        String sourcePath = executionParamDTO.getSourcePath();
        String destinationPath = executionParamDTO.getDestinationPath();

        ComparisonResultDTO comparisonResultDTO = md5DirectoryComparator.compare(sourcePath, destinationPath);
        log.info("{}{} between {} and {}, result: {}", LOG_PREFIX, "MD5Compare", sourcePath, destinationPath, comparisonResultDTO.toString());

        if (comparisonResultDTO.isIdentical()) {
            executionResultDTO.getTransmitResultDTO().setTransmitSuccess(Boolean.TRUE);
            executionResultDTO.getSolveDiffResultDTO().setSolveDiffSuccess(Boolean.TRUE);
        } else {
            executionResultDTO.getTransmitResultDTO().setTransmitSuccess(Boolean.FALSE);
            executionResultDTO.getSolveDiffResultDTO().setSolveDiffSuccess(Boolean.FALSE);
            LinkedHashMap<File, FileType> solveDiffFailureFileList = new LinkedHashMap<>();
            comparisonResultDTO.getDifferences().forEach(difference -> {
                solveDiffFailureFileList.put(new File(difference), FileType.FILE);
            });
            executionResultDTO.getSolveDiffResultDTO().setSolveDiffFailureFileList(solveDiffFailureFileList);
        }

        return executionResultDTO;
    }
}
