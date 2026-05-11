package com.job.distributed_job_scheduler.common;

public enum ExecutionType {
    HTTP,       // Gọi API webhook
    SCRIPT,     // Chạy shell script
    PYTHON,     // Chạy python script
    DOCKER      // Spin up một docker container để chạy task
}