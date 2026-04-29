package com.job.distributed_job_scheduler.core.model;

import com.job.distributed_job_scheduler.core.common.JobStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

import static com.job.distributed_job_scheduler.core.common.JobStatus.PENDING;

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

	private String name;

	private String description;

	@Column(name = "cron_expression")
	private String cronExpression;

	@JdbcTypeCode(SqlTypes.JSON) // Chỉ định cho Hibernate đây là kiểu JSON
	@Column(columnDefinition = "jsonb")
	private String payload;

	@Enumerated(EnumType.STRING)
	@ColumnDefault("PENDING")
	private JobStatus status = PENDING;

	@Column(name = "retry_limit")
	private Integer retryLimit;

	@Column(name = "retry_delay_seconds")
	private Integer retryDelaySeconds;

	@Column(name = "created_at")
	private Instant createdAt =  Instant.now();

	@Column(name = "updated_at")
	private Instant updatedAt;

	@Column(name = "retry_count")
	private Integer retryCount;

	@Column(name = "next_run_at")
	private Instant nextRunAt;

	@Column(name = "locked_at")
	private Instant lockedAt;

	@Column(name = "locked_by")
	private UUID lockedBy;

	public void markRunning(UUID workerId) {
		this.status = JobStatus.RUNNING;
		this.lockedBy = workerId;
	}

	public void markSuccess(UUID workerId) {
		this.status = JobStatus.SUCCESS;
		this.lockedAt = Instant.now();
		this.lockedBy = workerId;
	}

	public void markFailed() {
		if (retryCount < retryLimit) {
			retryCount++;
			this.status = JobStatus.RETRY;
			this.nextRunAt = Instant.now().plusSeconds(retryDelaySeconds);
		} else {
			this.status = JobStatus.FAILED;
		}
	}

}