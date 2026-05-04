package com.job.distributed_job_scheduler.core.service;

import com.job.distributed_job_scheduler.core.common.JobStatus;
import com.job.distributed_job_scheduler.core.model.Job;
import com.job.distributed_job_scheduler.core.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    // ==========================================
    //					CURD
    // ==========================================

    @Transactional
    public UUID createJob(Job job) {
        job.setStatus(JobStatus.ACTIVE);
        // Ở thực tế, bạn sẽ parse cronExpression để tính ra nextRunAt lần đầu tiên ở đây
        jobRepository.save(job);
        log.info("Created new Job with ID: {}", job.getId());
        return job.getId();
    }

    @Transactional
    public Job updateJob(UUID jobId, Job updatedJob) {
        Job existingJob = getJobById(jobId);

        existingJob.setName(updatedJob.getName());
        existingJob.setDescription(updatedJob.getDescription());
        existingJob.setCronExpression(updatedJob.getCronExpression());
        existingJob.setPayload(updatedJob.getPayload());
        existingJob.setExecutionType(updatedJob.getExecutionType());
        existingJob.setTimeoutSeconds(updatedJob.getTimeoutSeconds());
        existingJob.setMaxRetryCount(updatedJob.getMaxRetryCount());
        existingJob.setRetryBackoffSeconds(updatedJob.getRetryBackoffSeconds());
        existingJob.setQueueName(updatedJob.getQueueName());

        return jobRepository.save(existingJob);
    }

    @Transactional
    public void deleteJobById(UUID jobId) {
        Job job = getJobById(jobId);
        job.setStatus(JobStatus.DELETED);
        jobRepository.save(job);
        log.info("Soft deleted Job ID: {}", jobId);
    }

    public Job getJobById(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job with id " + jobId + " not found"));
    }

    public Boolean existsJob(UUID jobId) {
        return jobRepository.existsById(jobId);
    }

    // ==========================================
    // 2. NHÓM HÀM THAO TÁC TỪ DASHBOARD (UI)
    // ==========================================

    @Transactional
    public void pauseJob(UUID jobId) {
        Job job = getJobById(jobId);
        job.setStatus(JobStatus.PAUSED);
        job.setNextRunAt(null); // Xóa lịch chạy tiếp theo để Scheduler bỏ qua
        jobRepository.save(job);
        log.info("Paused Job ID: {}", jobId);
    }

    @Transactional
    public void resumeJob(UUID jobId, Instant nextRun) {
        Job job = getJobById(jobId);
        job.setStatus(JobStatus.ACTIVE);
        job.setNextRunAt(nextRun); // Tính toán lại lịch chạy
        jobRepository.save(job);
        log.info("Resumed Job ID: {}", jobId);
    }

    // ==========================================
    // 3. NHÓM HÀM HỖ TRỢ DISTRIBUTED SCHEDULER
    // ==========================================

    /**
     * Scheduler gọi hàm này để "khóa" (lock) job lại trước khi đẩy vào Message Queue / giao cho Worker
     * Tránh việc các Scheduler Node khác bốc nhầm.
     */
    @Transactional
    public boolean lockJobForExecution(UUID jobId, String schedulerNodeId) {
        Job job = getJobById(jobId);

        // Kiểm tra xem job có đang bị node khác khóa không
        if (job.getLockedBy() != null) {
            log.warn("Job {} is already locked by {}", jobId, job.getLockedBy());
            return false;
        }

        job.lockForExecution(schedulerNodeId);
        jobRepository.save(job); // Nhờ @Version trong Entity, nếu có concurrent update, nó sẽ quăng OptimisticLockException
        return true;
    }

    /**
     * Khi Worker chạy xong (hoặc lỗi), gọi hàm này để mở khóa Job
     * và cập nhật thời điểm chạy tiếp theo (tính toán từ Cron).
     */
    @Transactional
    public void releaseLockAndScheduleNext(UUID jobId, Instant nextRunAt) {
        Job job = getJobById(jobId);
        job.releaseLockAndSetNextRun(nextRunAt);
        jobRepository.save(job);
    }
}