package com.job.distributed_job_scheduler.controller;

import com.job.distributed_job_scheduler.service.CoreScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final CoreScheduler coreScheduler;

    /**
     * Dùng để test scheduler thủ công (debug)
     */
    @PostMapping("/run")
    public ResponseEntity<String> runSchedulerNow() {
        coreScheduler.scheduleDueJobs();
        return ResponseEntity.ok("Scheduler executed manually");
    }
}