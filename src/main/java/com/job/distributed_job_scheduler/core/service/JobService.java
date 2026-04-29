package com.job.distributed_job_scheduler.core.service;

import com.job.distributed_job_scheduler.core.common.JobStatus;
import com.job.distributed_job_scheduler.core.model.Job;
import com.job.distributed_job_scheduler.core.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {
	private final JobRepository jobRepository;

	public UUID createJob(Job job) {
		jobRepository.save(job);
		return job.getId();
	}

	public void updateJob(Job job) {
		if (jobRepository.findById(job.getId()).isPresent()) {
			job.setUpdatedAt(Instant.now());
			jobRepository.save(job);
			return;
		}
		throw new RuntimeException("Job with id " + job.getId() + " not found");
	}


	public void deleteJobById(UUID jobId) {
		jobRepository.deleteById(UUID.fromString(String.valueOf(jobId)));
	}

	public Job getJobById(UUID jobId) {
		return jobRepository.findById(jobId).orElse(null);
	}

	public Boolean existsJob(UUID jobId) {
		return jobRepository.existsById(jobId);
	}

	public void changeJobStatus(UUID jobId, JobStatus status, UUID workerID, Instant updatedAt) {
		switch (status) {
			case RETRY ->  {
				Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job with id " + jobId + " not found"));
				incrementRetryCount(jobId);
				job.markRunning(workerID);

			}

			case RUNNING ->  {
				Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job with id " + jobId + " not found"));
                job.setUpdatedAt(Instant.now());
				unlockJob(jobId, workerID);
				job.markRunning(workerID);
			}

			case SUCCESS ->  {
				Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job with id " + jobId + " not found"));
                job.markSuccess(workerID);
				lockJob(jobId, workerID);
			}

			case FAILED ->  {
                jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job with id " + jobId + " not found"));
				lockJob(jobId, workerID);
			}

			case PENDING ->  {
				jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job with id " + jobId + " not found"));
				updateNextRunAt(jobId, workerID, updatedAt);
			}

			default ->  {

			}
		}
	}

	public void incrementRetryCount(UUID jobId) {
		Job job = getJobById(jobId);
		if (job != null) {
			job.setRetryCount(job.getRetryCount() + 1);
			if(job.getRetryCount() >= 3) {
				job.markFailed();
			}
			updateJob(job);
		}
	}

	public void updateNextRunAt(UUID jobId, UUID workerId, Instant nextRunAt) {
		Job job = getJobById(jobId);
		if (job != null) {
			job.setNextRunAt(nextRunAt);
			job.markRunning(workerId);
			updateJob(job);
		}
	}

	public void lockJob(UUID jobId, UUID workerId) {
		Job job = getJobById(jobId);
		if (job != null) {
			job.setLockedAt(Instant.now());
			job.setLockedBy(workerId);
			updateJob(job);
		}
	}

	public void unlockJob(UUID jobId, UUID workerId) {
		Job job = getJobById(jobId);
		if (job != null) {
			if (job.getLockedAt() != null) {
				throw new RuntimeException("Job with id " + jobId + " isn't locked");
			}
			job.markRunning(workerId);
			updateJob(job);
		}
	}


}
