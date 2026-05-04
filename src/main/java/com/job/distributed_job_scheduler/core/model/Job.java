package com.job.distributed_job_scheduler.core.model;

import com.job.distributed_job_scheduler.core.common.ExecutionType;
import com.job.distributed_job_scheduler.core.common.JobStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "cron_expression")
    private String cronExpression;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_type", nullable = false)
    @Builder.Default
    private ExecutionType executionType = ExecutionType.HTTP;

    @Column(name = "queue_name", nullable = false)
    @Builder.Default
    private String queueName = "default";

    @Column(name = "timeout_seconds", nullable = false)
    @Builder.Default
    private Integer timeoutSeconds = 300;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobStatus status = JobStatus.ACTIVE;

    @Column(name = "max_retry_count", nullable = false)
    @Builder.Default
    private Integer maxRetryCount = 3;

    @Column(name = "retry_backoff_seconds", nullable = false)
    @Builder.Default
    private Integer retryBackoffSeconds = 60;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "locked_by")
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void lockForExecution(String nodeId) {
        this.lockedBy = nodeId;
        this.lockedAt = Instant.now();
    }

    public void releaseLockAndSetNextRun(Instant nextRun) {
        this.lockedBy = null;
        this.lockedAt = null;
        this.nextRunAt = nextRun;
    }
}