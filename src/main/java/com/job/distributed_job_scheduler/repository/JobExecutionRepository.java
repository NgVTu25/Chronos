package com.job.distributed_job_scheduler.repository;

import com.job.distributed_job_scheduler.model.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {
}
