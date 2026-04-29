package com.job.distributed_job_scheduler.core.repository;

import com.job.distributed_job_scheduler.core.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

}
