package com.job.distributed_job_scheduler.service;

import com.job.distributed_job_scheduler.common.ExecutionStatus;
import com.job.distributed_job_scheduler.common.WorkerStatus;
import com.job.distributed_job_scheduler.model.JobExecution;
import com.job.distributed_job_scheduler.model.Worker;
import com.job.distributed_job_scheduler.repository.JobExecutionRepository;
import com.job.distributed_job_scheduler.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final JobExecutionRepository jobExecutionRepository;
    private final JobExecutionService jobExecutionService;

    private UUID workerId = UUID.randomUUID(); // ID của worker này

    // ==========================================
    //					CURD
    // ==========================================

    public UUID CreateWorker(Worker worker) {
        workerRepository.save(worker);
        return worker.getId();
    }

    public Worker getWorkerById(UUID workerId) {
        return workerRepository.findById(workerId).orElseThrow(() -> new RuntimeException("Worker not found with ID: " + workerId));
    }

    public Worker updateWorker(UUID workerId, Worker updatedWorker) {
        Worker existingWorker = getWorkerById(workerId);
        existingWorker.setName(updatedWorker.getName());
        existingWorker.setStatus(updatedWorker.getStatus());
        return workerRepository.save(existingWorker);
    }

    public void deleteWorkerById(UUID workerId) {
        Worker worker = getWorkerById(workerId);
        workerRepository.delete(worker);
    }

    public Iterable<Worker> getAllWorkers() {
        return workerRepository.findAll();
    }

    // ==========================================
    // 2. NHÓM HÀM THAO TÁC TỬ DASHBOARD (UI)
    // ==========================================

    public void updateWorkerStatus(UUID workerId, WorkerStatus status) {
        Worker worker = getWorkerById(workerId);
        worker.setStatus(status);
        workerRepository.save(worker);
    }

    @Transactional
    public void updateWorkerHeartbeat(UUID workerId) {
        Worker worker = getWorkerById(workerId);
        worker.setLastHeartbeat(OffsetDateTime.now());
        workerRepository.save(worker);
    }

    // ==========================================
    // 3. WORKER EXECUTION LOGIC
    // ==========================================

    /**
     * Poll executions từ database và chạy chúng
     * Chạy mỗi 3 giây
     */
    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void pollAndExecuteJobs() {
        // Cập nhật heartbeat
        updateWorkerHeartbeat(workerId);
        updateWorkerStatus(workerId, WorkerStatus.ACTIVE);

        // Tìm tất cả executions ở trạng thái PENDING
        List<JobExecution> pendingExecutions = jobExecutionRepository
                .findAll()
                .stream()
                .filter(ex -> ex.getStatus() == ExecutionStatus.PENDING)
                .limit(5) // Xử lý tối đa 5 executions cùng lúc
                .toList();

        if (!pendingExecutions.isEmpty()) {
            log.info("Worker {} picked up {} pending executions", workerId, pendingExecutions.size());
        }

        for (JobExecution execution : pendingExecutions) {
            try {
                executeJob(execution);
            } catch (Exception e) {
                log.error("Error executing job execution {}: {}", execution.getId(), e.getMessage(), e);
                jobExecutionService.markAsFailed(execution.getId(), execution.getJob(), e.getMessage());
            }
        }

        updateWorkerStatus(workerId, WorkerStatus.IDLE);
    }

    /**
     * Thực thi một job
     */
    private void executeJob(JobExecution execution) throws Exception {
        log.info("Executing job {} (execution {})", execution.getJob().getId(), execution.getId());

        // Update status to RUNNING
        jobExecutionService.markAsRunning(execution.getId(), workerId);

        try {
            // Simulate job execution (în production, sẽ gọi API, run script, etc.)
            simulateJobExecution(execution);

            // Mark as SUCCESS
            jobExecutionService.markAsSuccess(execution.getId(), execution.getJob());
            log.info("Job execution {} completed successfully", execution.getId());

        } catch (Exception e) {
            log.error("Job execution {} failed: {}", execution.getId(), e.getMessage());
            jobExecutionService.markAsFailed(execution.getId(), execution.getJob(), e.getMessage());
            throw e;
        }
    }

    /**
     * Simulate job execution
     * Trong production, điều này sẽ call HTTP endpoint, run script, etc.
     */
    private void simulateJobExecution(JobExecution execution) throws Exception {
        log.info("Simulating job execution for job {}", execution.getJob().getId());

        // Simulate processing time
        Thread.sleep(1000);

        // Simulate random success/failure (90% success rate)
        if (Math.random() < 0.1) {
            throw new RuntimeException("Simulated job failure");
        }

        jobExecutionService.logExecution(execution, "INFO", "Job executed successfully");
        log.info("Simulated job execution completed");
    }

    /**
     * Get worker count
     */
    public long getWorkerCount() {
        return workerRepository.count();
    }

    /**
     * Get active worker count
     */
    public long getActiveWorkerCount() {
        return workerRepository
                .findAll()
                .stream()
                .filter(w -> w.getStatus() == WorkerStatus.ACTIVE)
                .count();
    }
}
