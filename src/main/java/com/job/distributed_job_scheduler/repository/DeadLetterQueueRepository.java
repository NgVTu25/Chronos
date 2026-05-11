package com.job.distributed_job_scheduler.repository;

import com.job.distributed_job_scheduler.model.DeadLetterQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeadLetterQueueRepository extends JpaRepository<DeadLetterQueue, UUID> {

    Optional<DeadLetterQueue> findByExecutionId(UUID executionId);

    @Query("SELECT COUNT(dlq) FROM DeadLetterQueue dlq WHERE dlq.resolvedAt IS NULL")
    long countUnresolved();
}

