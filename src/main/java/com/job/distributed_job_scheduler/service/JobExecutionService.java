package com.job.distributed_job_scheduler.service;

import com.job.distributed_job_scheduler.common.ExecutionStatus;
import com.job.distributed_job_scheduler.model.Job;
import com.job.distributed_job_scheduler.model.JobExecution;
import com.job.distributed_job_scheduler.model.ExecutionLog;
import com.job.distributed_job_scheduler.repository.JobExecutionRepository;
import com.job.distributed_job_scheduler.repository.ExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionService {

    private final JobExecutionRepository executionRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final JobService jobService;
    private final RetryService retryService;
    private final DeadLetterQueueService dlqService;

    /**
     * Hàm này được gọi bởi Scheduler khi một Job đến giờ chạy.
     * Tạo ra một Execution với trạng thái PENDING để đẩy vào Queue cho Worker xử lý.
     */
    @Transactional
    public void createExecutionForJob(Job job) {
        JobExecution execution = JobExecution.builder()
                .job(job)
                .status(ExecutionStatus.PENDING)
                .scheduledAt(Instant.now())
                .retryCount(0)
                .build();

        JobExecution savedExecution = executionRepository.save(execution);
        log.info("Created Execution ID {} for Job ID {}", savedExecution.getId(), job.getId());
    }

    /**
     * Dành cho nút "Run Now" trên Controller (Dashboard)
     */
    @Transactional
    public void triggerManualExecution(UUID jobId) {
        Job job = jobService.getJobById(jobId);
        createExecutionForJob(job);
        log.info("Manually triggered execution for Job ID {}", jobId);
    }

    /**
     * Worker cập nhật execution status thành RUNNING
     */
    @Transactional
    public void markAsRunning(UUID executionId, UUID workerId) {
        JobExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found"));

        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartedAt(Instant.now());
        executionRepository.save(execution);

        log.info("Marked execution {} as RUNNING", executionId);
    }

    /**
     * Worker cập nhật execution khi SUCCESS
     */
    @Transactional
    public void markAsSuccess(UUID executionId, Job job) {
        JobExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found"));

        execution.setStatus(ExecutionStatus.SUCCESS);
        execution.setEndedAt(Instant.now());
        executionRepository.save(execution);

        // Release lock và schedule next run
        jobService.releaseLockAndScheduleNext(job.getId(), null); // Will calculate next run from cron

        logExecution(execution, "INFO", "Execution completed successfully");
        log.info("Marked execution {} as SUCCESS", executionId);
    }

    /**
     * Worker cập nhật execution khi FAILED
     */
    @Transactional
    public void markAsFailed(UUID executionId, Job job, String errorMessage) {
        JobExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found"));

        execution.setStatus(ExecutionStatus.FAILED);
        execution.setEndedAt(Instant.now());
        execution.setErrorMessage(errorMessage);
        executionRepository.save(execution);

        logExecution(execution, "ERROR", "Execution failed: " + errorMessage);

        // Release lock
        jobService.releaseLockAndScheduleNext(job.getId(), null);

        // Thử thêm vào retry queue
        retryService.addToRetryQueue(execution, job);

        log.warn("Marked execution {} as FAILED: {}", executionId, errorMessage);
    }

    /**
     * Lưu execution log
     */
    @Transactional
    public void logExecution(JobExecution execution, String logLevel, String message) {
        ExecutionLog log = ExecutionLog.builder()
                .execution(execution)
                .logLevel(logLevel)
                .message(message)
                .build();

        executionLogRepository.save(log);
    }

    /**
     * Lấy execution theo ID
     */
    public JobExecution getExecutionById(UUID executionId) {
        return executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found"));
    }
}