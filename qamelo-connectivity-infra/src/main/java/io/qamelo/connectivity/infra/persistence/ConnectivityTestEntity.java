package io.qamelo.connectivity.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connectivity_tests")
public class ConnectivityTestEntity {

    @Id
    public UUID id;

    @Column(name = "connection_id", nullable = false)
    public UUID connectionId;

    @Column(nullable = false)
    public String direction;

    @Column(nullable = false)
    public String type;

    @Column(nullable = false)
    public String status;

    @Column(name = "result_message")
    public String resultMessage;

    @Column(name = "latency_ms")
    public Long latencyMs;

    @Column(name = "error_detail")
    public String errorDetail;

    @Column(name = "started_at", nullable = false)
    public Instant startedAt;

    @Column(name = "completed_at")
    public Instant completedAt;

    @Column(name = "initiated_by")
    public String initiatedBy;
}
