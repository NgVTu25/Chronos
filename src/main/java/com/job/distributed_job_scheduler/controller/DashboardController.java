package com.job.distributed_job_scheduler.controller;

import com.job.distributed_job_scheduler.common.ExecutionStatus;
import com.job.distributed_job_scheduler.model.dtos.ApiResponseDto;
import com.job.distributed_job_scheduler.repository.DeadLetterQueueRepository;
import com.job.distributed_job_scheduler.repository.JobExecutionRepository;
import com.job.distributed_job_scheduler.repository.JobRepository;
import com.job.distributed_job_scheduler.repository.RetryQueueRepository;
import com.job.distributed_job_scheduler.service.WorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final JobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final RetryQueueRepository retryQueueRepository;
    private final DeadLetterQueueRepository dlqRepository;
    private final WorkerService workerService;

    /**
     * Lấy tóm tắt dashboard
     * API: GET /api/v1/dashboard/summary
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'VIEWER')")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getDashboardSummary() {
        log.info("Fetching dashboard summary");

        Map<String, Object> summary = new HashMap<>();

        // Job statistics
        summary.put("total_jobs", jobRepository.count());

        // Execution statistics
        long totalExecutions = executionRepository.count();
        summary.put("total_executions", totalExecutions);

        long runningExecutions = executionRepository.findAll().stream()
                .filter(ex -> ex.getStatus() == ExecutionStatus.RUNNING)
                .count();
        summary.put("running_executions", runningExecutions);

        long failedExecutions = executionRepository.findAll().stream()
                .filter(ex -> ex.getStatus() == ExecutionStatus.FAILED)
                .count();
        summary.put("failed_executions", failedExecutions);

        long successExecutions = executionRepository.findAll().stream()
                .filter(ex -> ex.getStatus() == ExecutionStatus.SUCCESS)
                .count();
        summary.put("success_executions", successExecutions);

        // Queue statistics
        summary.put("retry_queue_size", retryQueueRepository.count());
        summary.put("dlq_size", dlqRepository.countUnresolved());

        // Worker statistics
        summary.put("worker_count", workerService.getWorkerCount());
        summary.put("active_workers", workerService.getActiveWorkerCount());

        return ResponseEntity.ok(ApiResponseDto.success("Dashboard summary retrieved", summary));
    }

    /**
     * Lấy metrics chi tiết
     * API: GET /api/v1/dashboard/metrics
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'VIEWER')")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getMetrics() {
        log.info("Fetching detailed metrics");

        Map<String, Object> metrics = new HashMap<>();

        // Count by status
        Map<String, Long> executionsByStatus = new HashMap<>();
        for (ExecutionStatus status : ExecutionStatus.values()) {
            executionsByStatus.put(status.toString(), executionRepository.findAll().stream()
                    .filter(ex -> ex.getStatus() == status)
                    .count());
        }
        metrics.put("executions_by_status", executionsByStatus);

        // Queue depth
        metrics.put("pending_count", executionRepository.findAll().stream()
                .filter(ex -> ex.getStatus() == ExecutionStatus.PENDING)
                .count());

        metrics.put("retrying_count", executionRepository.findAll().stream()
                .filter(ex -> ex.getStatus() == ExecutionStatus.RETRYING)
                .count());

        return ResponseEntity.ok(ApiResponseDto.success("Metrics retrieved", metrics));
    }
}

