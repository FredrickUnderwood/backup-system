package com.uestc.backupsystem.controller;

import com.uestc.backupsystem.dto.*;
import com.uestc.backupsystem.service.CaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CaseController {

    @Autowired
    private CaseService caseService;

    @PostMapping("/execute")
    public ResponseEntity<String> createNewExecution(@RequestBody CaseExecutionParamDTO caseExecutionParam) {
        String resultMap = caseService.createNewExecution(caseExecutionParam);
        if(resultMap != null) {
            return ResponseEntity.ok(resultMap);
        } else {
            return ResponseEntity.badRequest().body("Execution failed.");
        }
    }

    @PostMapping("/create-new-case")
    public ResponseEntity<String> createNewCase(@RequestBody CreateNewCaseParamDTO createNewCaseParam) {
        String resultMap = caseService.createNewCase(createNewCaseParam);
        if(resultMap != null) {
            return ResponseEntity.ok(resultMap);
        } else {
            return ResponseEntity.badRequest().body("Create new case failed.");
        }
    }

    @PostMapping("/update-case-backup-path")
    public ResponseEntity<String> updateCaseBackupPath(@RequestBody UpdateCaseBackupPathParamDTO updateCaseBackupPathParam) {
        String resultMap = caseService.updateCaseBackupPath(updateCaseBackupPathParam);
        if(resultMap != null) {
            return ResponseEntity.ok(resultMap);
        } else {
            return ResponseEntity.badRequest().body("Update case back up path failed.");
        }
    }

    @GetMapping("/")
    public ResponseEntity<String> getAllCaseRecords() {
        String resultMap = caseService.getAllCaseRecords();
        if(resultMap != null) {
            return ResponseEntity.ok(resultMap);
        } else {
            return ResponseEntity.badRequest().body("Get all case records failed.");
        }
    }

    @GetMapping("/{case_id}")
    public ResponseEntity<String> getAllExecutionRecordsByCaseId(@PathVariable("case_id") long caseId) {
        String resultMap = caseService.getAllExecutionRecordsByCaseId(caseId);
        if(resultMap != null) {
            return ResponseEntity.ok(resultMap);
        } else {
            return ResponseEntity.badRequest().body("Get all execution records by caseId failed.");
        }
    }

    @PostMapping("/delete-case")
    public ResponseEntity<String> deleteCaseById(@RequestBody DeleteCaseParamDTO deleteCaseParamDTO) {
        String resultMap = caseService.deleteCase(deleteCaseParamDTO);
        if(resultMap != null) {
            return ResponseEntity.ok(resultMap);
        } else {
            return ResponseEntity.badRequest().body("Delete case failed.");
        }
    }

}
