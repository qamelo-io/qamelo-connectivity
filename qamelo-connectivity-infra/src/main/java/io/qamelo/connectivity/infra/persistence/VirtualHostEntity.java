package io.qamelo.connectivity.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_virtual_hosts")
public class VirtualHostEntity {

    @Id
    public UUID id;

    @Column(name = "agent_id", nullable = false)
    public UUID agentId;

    @Column(nullable = false)
    public String hostname;

    @Column(name = "target_host", nullable = false)
    public String targetHost;

    @Column(name = "target_port", nullable = false)
    public int targetPort;

    @Column(nullable = false)
    public String protocol;

    @Column(nullable = false)
    public String status;

    @Column(name = "connection_id")
    public UUID connectionId;

    @Column(name = "circuit_breaker_threshold")
    public int circuitBreakerThreshold;

    @Column(name = "timeout_seconds")
    public int timeoutSeconds;

    @Column(name = "max_concurrent_connections")
    public int maxConcurrentConnections;

    @Column(name = "retry_max_attempts")
    public int retryMaxAttempts;

    @Column(name = "retry_backoff_ms")
    public int retryBackoffMs;

    @Column(name = "pool_max_connections")
    public int poolMaxConnections;

    @Column(name = "pool_keep_alive_seconds")
    public int poolKeepAliveSeconds;

    @Column(name = "pool_idle_timeout_seconds")
    public int poolIdleTimeoutSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "created_by")
    public String createdBy;

    @Column(name = "modified_at")
    public Instant modifiedAt;

    @Column(name = "modified_by")
    public String modifiedBy;
}
