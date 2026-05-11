package com.job.distributed_job_scheduler.service;

import com.job.distributed_job_scheduler.common.ExecutionStatus;
import com.job.distributed_job_scheduler.model.DeadLetterQueue;
import com.job.distributed_job_scheduler.model.JobExecution;
import com.job.distributed_job_scheduler.repository.DeadLetterQueueRepository;
import com.job.distributed_job_scheduler.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService {

    private final DeadLetterQueueRepository dlqRepository;
    private final JobExecutionRepository executionRepository;

    /**
     * Đưa execution vào Dead Letter Queue khi vượt quá số lần retry
     */
    @Transactional
    public void moveExecutionToDLQ(JobExecution execution, String reason) {
        log.warn("Moving execution {} to DLQ. Reason: {}", execution.getId(), reason);

        // Update execution status to DEAD_LETTER
        execution.setStatus(ExecutionStatus.DEAD_LETTER);
        execution.setEndedAt(Instant.now());
        executionRepository.save(execution);

        // Tạo DLQ entry
        DeadLetterQueue dlq = DeadLetterQueue.builder()
                .execution(execution)
                .reason(reason)
                .finalErrorMessage(execution.getErrorMessage())
                .build();

        dlqRepository.save(dlq);
        log.info("Execution {} moved to DLQ", execution.getId());
    }

    /**
     * Admin retry execution từ DLQ
     */
    @Transactional
    public void retryFromDLQ(UUID dlqId, UUID adminUserId) {
        DeadLetterQueue dlq = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new RuntimeException("DLQ entry not found"));

        if (dlq.getResolvedAt() != null) {
            throw new RuntimeException("This DLQ entry has already been resolved");
        }

        JobExecution execution = dlq.getExecution();

        // Reset execution để retry
        execution.setStatus(ExecutionStatus.PENDING);
        execution.setRetryCount(0);
        execution.setErrorMessage(null);
        execution.setStartedAt(null);
        execution.setEndedAt(null);
        executionRepository.save(execution);

        // Mark DLQ as resolved
        dlq.setResolvedAt(Instant.now());
        dlq.setResolvedBy(adminUserId);
        dlq.setResolutionNote("Manually retried by admin");
        dlqRepository.save(dlq);

        log.info("Execution {} retried from DLQ by admin {}", execution.getId(), adminUserId);
    }

    /**
     * Lấy số lượng execution trong DLQ chưa resolved
     */
    public long getUnresolvedDLQSize() {
        return dlqRepository.countUnresolved();
    }
}

