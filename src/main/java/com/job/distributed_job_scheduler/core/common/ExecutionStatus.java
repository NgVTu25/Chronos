package com.job.distributed_job_scheduler.core.common;

public enum ExecutionStatus {
	PENDING,
	RUNNING,
	SUCCESS,
	FAILED,
	RETRYING,
	DEAD_LETTER
}