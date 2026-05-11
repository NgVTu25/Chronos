package com.job.distributed_job_scheduler.controller;

import com.job.distributed_job_scheduler.model.Job;
import com.job.distributed_job_scheduler.model.dtos.ApiResponseDto;
import com.job.distributed_job_scheduler.model.dtos.JobDto;
import com.job.distributed_job_scheduler.service.JobService;
import com.job.distributed_job_scheduler.service.JobExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobExecutionService jobExecutionService;
    private final ModelMapper modelMapper;

    /**
     * Tạo mới một Job Configuration
     * API: POST /api/v1/jobs
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    public ResponseEntity<ApiResponseDto<JobDto>> createJob(@RequestBody JobDto dto) {
        log.info("Creating new job: {}", dto.getName());
        Job jobToCreate = modelMapper.map(dto, Job.class);
        UUID createdJobId = jobService.createJob(jobToCreate);
        Job createdJob = jobService.getJobById(createdJobId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Job created successfully", modelMapper.map(createdJob, JobDto.class)));
    }

    /**
     * Lấy chi tiết một Job
     * API: GET /api/v1/jobs/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'VIEWER')")
    public ResponseEntity<ApiResponseDto<JobDto>> getJobById(@PathVariable UUID id) {
        try {
            log.info("Fetching job with ID: {}", id);
            Job job = jobService.getJobById(id);
            return ResponseEntity.ok(ApiResponseDto.success("Job retrieved successfully", modelMapper.map(job, JobDto.class)));
        } catch (RuntimeException e) {
            log.warn("Job not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDto.error("JOB_NOT_FOUND", "Job not found"));
        }
    }

    /**
     * Cập nhật toàn bộ cấu hình một Job
     * API: PUT /api/v1/jobs/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    public ResponseEntity<ApiResponseDto<JobDto>> updateJob(@PathVariable UUID id, @RequestBody JobDto dto) {
        try {
            log.info("Updating job with ID: {}", id);
            Job updatedJobInfo = modelMapper.map(dto, Job.class);
            Job savedJob = jobService.updateJob(id, updatedJobInfo);
            return ResponseEntity.ok(ApiResponseDto.success("Job updated successfully", modelMapper.map(savedJob, JobDto.class)));
        } catch (RuntimeException e) {
            log.warn("Failed to update job: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDto.error("JOB_NOT_FOUND", "Job not found"));
        }
    }

    /**
     * Xóa mềm (Soft Delete) một Job
     * API: DELETE /api/v1/jobs/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> deleteJob(@PathVariable UUID id) {
        log.info("Request to delete Job ID: {}", id);
        try {
            jobService.deleteJobById(id);
            return ResponseEntity.ok(ApiResponseDto.success("Job deleted successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDto.error("JOB_NOT_FOUND", "Job not found"));
        }
    }

    /**
     * Tạm dừng Job
     * API: POST /api/v1/jobs/{id}/pause
     */
    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    public ResponseEntity<ApiResponseDto<Void>> pauseJob(@PathVariable("id") UUID id) {
        try {
            log.info("Pausing job with ID: {}", id);
            jobService.pauseJob(id);
            return ResponseEntity.ok(ApiResponseDto.success("Job paused successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDto.error("JOB_NOT_FOUND", "Job not found"));
        }
    }

    /**
     * Tiếp tục chạy Job
     * API: POST /api/v1/jobs/{id}/resume
     */
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    public ResponseEntity<ApiResponseDto<Void>> resumeJob(@PathVariable UUID id,
                                             @RequestParam(required = false) Instant nextRunAt) {
        try {
            log.info("Resuming job with ID: {}", id);
            Instant runTime = (nextRunAt != null) ? nextRunAt : Instant.now();
            jobService.resumeJob(id, runTime);
            return ResponseEntity.ok(ApiResponseDto.success("Job resumed successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDto.error("JOB_NOT_FOUND", "Job not found"));
        }
    }

    /**
     * Chạy Job ngay lập tức (Run Now)
     * API: POST /api/v1/jobs/{id}/run
     */
    @PostMapping("/{id}/run")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    public ResponseEntity<ApiResponseDto<UUID>> runJobImmediately(@PathVariable("id") UUID id) {
        try {
            log.info("Running job immediately with ID: {}", id);
            jobExecutionService.triggerManualExecution(id);
            return ResponseEntity.accepted()
                    .body(ApiResponseDto.success("Job execution triggered", id));
        } catch (RuntimeException e) {
            log.error("Failed to run job: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDto.error("JOB_NOT_FOUND", "Job not found"));
        }
    }
}