package com.job.distributed_job_scheduler.core.repository;

import com.job.distributed_job_scheduler.core.model.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {
}
