package com.job.distributed_job_scheduler.core.model;

import com.job.distributed_job_scheduler.core.common.WorkerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Worker {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "host_address")
    private String hostAddress;

    @Enumerated(EnumType.STRING)
    private WorkerStatus status = WorkerStatus.IDLE;

    @Column(name = "last_heartbeat")
    private OffsetDateTime lastHeartbeat;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}