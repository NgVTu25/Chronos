CREATE TABLE workers
(
    id             UUID         NOT NULL,
    name           VARCHAR(255) NOT NULL,
    host_address   VARCHAR(255),
    status         VARCHAR(255),
    last_heartbeat TIMESTAMP WITHOUT TIME ZONE,
    updated_at     TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_workers PRIMARY KEY (id)
);