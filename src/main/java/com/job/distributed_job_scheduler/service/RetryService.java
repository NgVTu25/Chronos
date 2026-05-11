package com.job.distributed_job_scheduler.service;

import com.job.distributed_job_scheduler.common.ExecutionStatus;
import com.job.distributed_job_scheduler.model.Job;
import com.job.distributed_job_scheduler.model.JobExecution;
import com.job.distributed_job_scheduler.model.RetryQueue;
import com.job.distributed_job_scheduler.repository.JobExecutionRepository;
import com.job.distributed_job_scheduler.repository.RetryQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryService {

    private final RetryQueueRepository retryQueueRepository;
    private final JobExecutionRepository jobExecutionRepository;
    private final JobExecutionService jobExecutionService;
    private final DeadLetterQueueService dlqService;

    /**
     * Đưa execution thất bại vào retry queue với backoff delay
     */
    @Transactional
    public void addToRetryQueue(JobExecution execution, Job job) {
        int retryCount = execution.getRetryCount();

        if (retryCount >= job.getMaxRetryCount()) {
            log.warn("Execution {} exceeded max retries. Moving to DLQ.", execution.getId());
            dlqService.moveExecutionToDLQ(execution, "Exceeded max retry count");
            return;
        }

        // Tính toán exponential backoff: base * (2 ^ retry_count)
        long delaySeconds = calculateBackoffDelay(job.getRetryBackoffSeconds(), retryCount);
        Instant nextRetryAt = Instant.now().plusSeconds(delaySeconds);

        // Tạo retry queue entry
        RetryQueue retryQueue = RetryQueue.builder()
                .execution(execution)
                .retryCount(retryCount)
                .maxRetryCount(job.getMaxRetryCount())
                .nextRetryAt(nextRetryAt)
                .backoffSeconds((int) delaySeconds)
                .build();

        retryQueueRepository.save(retryQueue);

        // Update execution status to RETRYING
        execution.setStatus(ExecutionStatus.RETRYING);
        jobExecutionRepository.save(execution);

        log.info("Execution {} added to retry queue. Next retry at: {}", execution.getId(), nextRetryAt);
    }

    /**
     * Poll retry queue và tạo lại execution khi đến hạn retry
     */
    @Scheduled(fixedDelay = 10000) // Check mỗi 10s
    @Transactional
    public void processRetryQueue() {
        Instant now = Instant.now();
        List<RetryQueue> dueRetries = retryQueueRepository.findDueForRetry(now);

        if (!dueRetries.isEmpty()) {
            log.info("Found {} executions due for retry", dueRetries.size());
        }

        for (RetryQueue retryQueue : dueRetries) {
            try {
                JobExecution execution = retryQueue.getExecution();

                // Increment retry count
                execution.setRetryCount(execution.getRetryCount() + 1);

                // Reset execution status to PENDING để worker pick up lại
                execution.setStatus(ExecutionStatus.PENDING);
                execution.setStartedAt(null);
                execution.setEndedAt(null);
                execution.setErrorMessage(null);

                jobExecutionRepository.save(execution);

                // Delete từ retry queue
                retryQueueRepository.delete(retryQueue);

                log.info("Retried execution {} (attempt #{})", execution.getId(), execution.getRetryCount());
            } catch (Exception e) {
                log.error("Error processing retry for execution {}: {}", retryQueue.getExecution().getId(), e.getMessage());
            }
        }
    }

    /**
     * Tính toán exponential backoff delay
     * Formula: baseDelay * (2 ^ attempt)
     */
    private long calculateBackoffDelay(int baseDelaySeconds, int attemptNumber) {
        long delay = (long) (baseDelaySeconds * Math.pow(2, attemptNumber));
        // Cap maximum delay to 1 hour (3600 seconds)
        return Math.min(delay, 3600);
    }

    /**
     * Lấy số lượng execution đang chờ retry
     */
    public long getRetryQueueSize() {
        return retryQueueRepository.count();
    }
}

