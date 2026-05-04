package com.job.distributed_job_scheduler.core.service;

import com.job.distributed_job_scheduler.core.common.ExecutionStatus;
import com.job.distributed_job_scheduler.core.model.Job;
import com.job.distributed_job_scheduler.core.model.JobExecution;
import com.job.distributed_job_scheduler.core.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionService {

    private final JobExecutionRepository executionRepository;
    private final JobService jobService;

    /**
     * Hàm này được gọi bởi Scheduler khi một Job đến giờ chạy.
     * Tạo ra một Execution với trạng thái PENDING để đẩy vào Queue cho Worker xử lý.
     */
    @Transactional
    public JobExecution createExecutionForJob(Job job) {
        JobExecution execution = JobExecution.builder()
                .job(job)
                .status(ExecutionStatus.PENDING)
                .scheduledAt(Instant.now())
                .retryCount(0)
                .build();

        JobExecution savedExecution = executionRepository.save(execution);
        log.info("Created Execution ID {} for Job ID {}", savedExecution.getId(), job.getId());
        return savedExecution;
    }

    /**
     * Dành cho nút "Run Now" trên Controller (Dashboard)
     */
    @Transactional
    public void triggerManualExecution(java.util.UUID jobId) {
        Job job = jobService.getJobById(jobId);
        createExecutionForJob(job);
        log.info("Manually triggered execution for Job ID {}", jobId);
    }
}