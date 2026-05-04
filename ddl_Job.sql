CREATE TABLE jobs
(
    id                  UUID NOT NULL,
    name                VARCHAR(255),
    description         VARCHAR(255),
    cron_expression     VARCHAR(255),
    payload             JSONB,
    status              VARCHAR(255) DEFAULT 'PENDING',
    retry_limit         INTEGER,
    retry_delay_seconds INTEGER,
    created_at          TIMESTAMP WITHOUT TIME ZONE,
    updated_at          TIMESTAMP WITHOUT TIME ZONE,
    retry_count         INTEGER,
    next_run_at         TIMESTAMP WITHOUT TIME ZONE,
    locked_at           TIMESTAMP WITHOUT TIME ZONE,
    locked_by           UUID,
    CONSTRAINT pk_jobs PRIMARY KEY (id)
);