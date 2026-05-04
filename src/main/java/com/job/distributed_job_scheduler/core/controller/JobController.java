package com.job.distributed_job_scheduler.core.controller;

import com.job.distributed_job_scheduler.core.model.Job;
import com.job.distributed_job_scheduler.core.model.dtos.JobDto;
import com.job.distributed_job_scheduler.core.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final ModelMapper modelMapper;

    /**
     * Tạo mới một Job Configuration
     * API: POST /api/jobs
     */
    @PostMapping
    public ResponseEntity<UUID> createJob(@RequestBody JobDto dto) {
        Job jobToCreate = modelMapper.map(dto, Job.class);
        UUID createdJobId = jobService.createJob(jobToCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdJobId);
    }

    /**
     * Lấy thông tin chi tiết một Job
     * API: GET /api/jobs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobDto> getJobById(@PathVariable UUID id) {
        try {
            Job job = jobService.getJobById(id);
            return ResponseEntity.ok(modelMapper.map(job, JobDto.class));
        } catch (RuntimeException e) {
            log.warn("Job not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Cập nhật toàn bộ cấu hình một Job
     * API: PUT /api/jobs/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<JobDto> updateJob(@PathVariable UUID id, @RequestBody JobDto dto) {
        try {
            Job updatedJobInfo = modelMapper.map(dto, Job.class);
            Job savedJob = jobService.updateJob(id, updatedJobInfo);
            return ResponseEntity.ok(modelMapper.map(savedJob, JobDto.class));
        } catch (RuntimeException e) {
            log.warn("Failed to update job: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Xóa mềm (Soft Delete) một Job
     * API: DELETE /api/jobs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteJob(@PathVariable UUID id) {
        log.info("Request to delete Job ID: {}", id);
        try {
            jobService.deleteJobById(id);
            return ResponseEntity.ok("Job deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Job not found");
        }
    }

    // ==========================================
    // CÁC ENDPOINT CHO DASHBOARD ACTIONS
    // ==========================================

    /**
     * Tạm dừng Job (Không cho scheduler lên lịch nữa)
     * API: POST /api/jobs/{id}/pause
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<String> pauseJob(@PathVariable("id") UUID id) {
        try {
            jobService.pauseJob(id);
            return ResponseEntity.ok("Job paused successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Job not found");
        }
    }

    /**
     * Tiếp tục chạy Job
     * API: POST /api/jobs/{id}/resume
     */
    @PostMapping("/{id}/resume")
    public ResponseEntity<String> resumeJob(@PathVariable UUID id,
                                            @RequestParam(required = false) Instant nextRunAt) {
        try {
            Instant runTime = (nextRunAt != null) ? nextRunAt : Instant.now();
            jobService.resumeJob(id, runTime);
            return ResponseEntity.ok("Job resumed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Job not found");
        }
    }

    /**
     * CHÚ Ý: Chức năng "Run Now" (Chạy ngay)
     * API: POST /api/jobs/{id}/run
     * <p>
     * Logic này thực chất là tạo ra một BẢN GHI JOB EXECUTION mới ngay lập tức
     * chứ KHÔNG PHẢI sửa status của bảng Job.
     * Do đó, nó nên được chuyển giao cho JobExecutionService xử lý (sẽ viết sau).
     */
    @PostMapping("/{id}/run")
    public ResponseEntity<String> runJobImmediately(@PathVariable("id") UUID id) {
        // TODO: Chuyển logic này sang JobExecutionService
        // jobExecutionService.triggerManualExecution(id);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("Tính năng đang được phát triển ở nhánh JobExecution");
    }
}