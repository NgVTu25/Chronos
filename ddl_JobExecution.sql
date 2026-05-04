CREATE TABLE job_executions
(
    id            UUID         NOT NULL,
    job_id        UUID,
    worker_id     UUID,
    status        VARCHAR(255) NOT NULL,
    retry_count   INTEGER,
    error_message TEXT,
    started_at    TIMESTAMP WITHOUT TIME ZONE,
    ended_at      TIMESTAMP WITHOUT TIME ZONE,
    created_at    TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_job_executions PRIMARY KEY (id)
);

CREATE INDEX idx_job_executions_status ON job_executions (status);

ALTER TABLE job_executions
    ADD CONSTRAINT FK_JOB_EXECUTIONS_ON_JOB FOREIGN KEY (job_id) REFERENCES jobs (id);

CREATE INDEX idx_job_executions_job_id ON job_executions (job_id);

ALTER TABLE job_executions
    ADD CONSTRAINT FK_JOB_EXECUTIONS_ON_WORKER FOREIGN KEY (worker_id) REFERENCES workers (id);