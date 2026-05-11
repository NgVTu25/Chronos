package com.job.distributed_job_scheduler.repository;

import com.job.distributed_job_scheduler.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    // Tìm các job đến giờ chạy và chưa bị node nào lock
    @Query("SELECT j FROM Job j WHERE j.status = 'ACTIVE' " +
            "AND (j.nextRunAt IS NULL OR j.nextRunAt <= :now) " +
            "AND j.lockedBy IS NULL")
    List<Job> findDueJobs(@Param("now") Instant now);
}