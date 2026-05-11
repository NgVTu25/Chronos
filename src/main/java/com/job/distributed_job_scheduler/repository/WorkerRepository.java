package com.job.distributed_job_scheduler.repository;

import com.job.distributed_job_scheduler.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkerRepository extends JpaRepository<Worker, UUID> {

}
