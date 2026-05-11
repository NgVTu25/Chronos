package com.job.distributed_job_scheduler.model.dtos;

import com.job.distributed_job_scheduler.common.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobDto implements Serializable {
    private UUID id;
    private String name;
    private String description;
    private String cronExpression;
    private String payload;
    private JobStatus status;
    private Integer retryLimit;
    private Integer retryDelaySeconds;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lockedAt;
    private UUID lockedBy;
}