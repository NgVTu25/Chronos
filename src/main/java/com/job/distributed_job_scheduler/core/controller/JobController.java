package com.job.distributed_job_scheduler.core.controller;

import com.job.distributed_job_scheduler.core.common.JobStatus;
import com.job.distributed_job_scheduler.core.model.Job;
import com.job.distributed_job_scheduler.core.model.dtos.JobDto;
import com.job.distributed_job_scheduler.core.service.JobService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {
    private final JobService jobService;
    private final ModelMapper modelMapper;

    @PostMapping("/create")
    public ResponseEntity<UUID> createJob(@RequestBody JobDto dto) {
        UUID id = jobService.createJob(modelMapper.map(dto, Job.class));
        return  new ResponseEntity<>(id, HttpStatus.CREATED);
    }

    @GetMapping("/get/{JobId}")
    public ResponseEntity<JobDto> getJobById(@PathVariable("JobId") UUID id) {
        JobDto jobDto = modelMapper.map(jobService.getJobById(id), JobDto.class);
    	if (jobDto != null) {
    		return new ResponseEntity<>(jobDto, HttpStatus.OK);
    	} else {
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	}
    }

    @PutMapping("/update/{JobId}")
    public ResponseEntity<JobDto> updateJob(@RequestBody JobDto dto, @PathVariable("JobId") UUID id) {
        if (!jobService.existsJob(id)) {
            jobService.updateJob(modelMapper.map(dto, Job.class));
            return ResponseEntity.status(200).body(dto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/delete/{JobId}")
    public ResponseEntity<?> deleteJob(@PathVariable("JobId") UUID id) {
        System.out.println(">>> Đang thử xóa Job ID: " + id);

        Job job = jobService.getJobById(id);
        if (job == null) {
            System.out.println(">>> LỖI: Không tìm thấy Job này trong DB!");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy Job với ID: " + id);
        }

        jobService.deleteJobById(id);
        return ResponseEntity.ok(modelMapper.map(job, JobDto.class));
    }

    @PutMapping("/status/{JobId}/{JobStatus}/{WorkerID}")
    public ResponseEntity<JobDto> updateJobStatus(@PathVariable("JobId") UUID id,
                                                  @PathVariable("JobStatus") JobStatus status,
                                                  @PathVariable("WorkerID") UUID workerID, @RequestParam Instant timestamp
                                                  ) {
        if (!jobService.existsJob(id)) {
            jobService.changeJobStatus(id, status, workerID,  timestamp);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/run/{JobId}")
    public ResponseEntity<JobDto> runJob(@PathVariable("JobId") UUID id) {
        if (jobService.existsJob(id)) {
            Job job = jobService.getJobById(id);
            job.markRunning(id);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
