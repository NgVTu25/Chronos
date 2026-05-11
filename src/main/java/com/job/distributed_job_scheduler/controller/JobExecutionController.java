package com.job.distributed_job_scheduler.controller;

import com.job.distributed_job_scheduler.service.JobExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/executions")
@RequiredArgsConstructor
public class JobExecutionController {

    private final JobExecutionService jobExecutionService;

    /**
     * API cho nút "Run Now" từ Dashboard
     */
    @PostMapping("/trigger/{jobId}")
    public ResponseEntity<String> triggerExecution(@PathVariable UUID jobId) {
        jobExecutionService.triggerManualExecution(jobId);
        return ResponseEntity.ok("Execution triggered for Job ID: " + jobId);
    }
}