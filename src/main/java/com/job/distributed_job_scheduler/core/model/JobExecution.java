package com.job.distributed_job_scheduler.core.model;

import com.job.distributed_job_scheduler.core.common.ExecutionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_executions",
		indexes = {
				@Index(name = "idx_job_executions_job_id", columnList = "job_id"),
				@Index(name = "idx_job_executions_status", columnList = "status")
		})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobExecution {

	@Id
	@GeneratedValue
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "job_id")
	private Job job;

	@ManyToOne
	@JoinColumn(name = "worker_id")
	private Worker worker;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ExecutionStatus status;

	@Column(name = "retry_count")
	private Integer retryCount = 0;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	@Column(name = "started_at")
	private OffsetDateTime startedAt;

	@Column(name = "ended_at")
	private OffsetDateTime endedAt;

	@Column(name = "created_at")
	private OffsetDateTime createdAt;
}