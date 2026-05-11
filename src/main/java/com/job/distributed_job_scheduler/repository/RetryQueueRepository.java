package com.job.distributed_job_scheduler.repository;

import com.job.distributed_job_scheduler.model.RetryQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RetryQueueRepository extends JpaRepository<RetryQueue, UUID> {

    Optional<RetryQueue> findByExecutionId(UUID executionId);

    @Query("SELECT rq FROM RetryQueue rq WHERE rq.nextRetryAt <= :now ORDER BY rq.nextRetryAt ASC")
    List<RetryQueue> findDueForRetry(@Param("now") Instant now);

    long countByNextRetryAtLessThanEqual(Instant now);
}

