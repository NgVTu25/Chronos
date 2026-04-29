package com.job.distributed_job_scheduler.core.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "execution_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "execution_id")
	private JobExecution execution;

	@Column(name = "log_level")
	private String logLevel;

	@Column(columnDefinition = "TEXT")
	private String message;

	@Column(name = "created_at")
	private OffsetDateTime createdAt;
}