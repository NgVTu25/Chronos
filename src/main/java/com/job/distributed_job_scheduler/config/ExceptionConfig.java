package com.job.distributed_job_scheduler.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Configuration
public class ExceptionConfig {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleError(Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }



}
