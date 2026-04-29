package com.job.distributed_job_scheduler.core.common;

public enum JobStatus {
	PENDING,
	RUNNING,
	SUCCESS,
	FAILED,
	RETRY;

	public boolean isFinal() {
		return this == SUCCESS || this == FAILED;
	}
}