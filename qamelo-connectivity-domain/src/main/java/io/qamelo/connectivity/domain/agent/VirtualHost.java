package io.qamelo.connectivity.domain.agent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class VirtualHost {

    private final UUID id;
    private UUID agentId;
    private String hostname;
    private String targetHost;
    private int targetPort;
    private VirtualHostProtocol protocol;
    private VirtualHostStatus status;
    private UUID connectionId;
    // Resiliency config
    private int circuitBreakerThreshold;
    private int timeoutSeconds;
    private int maxConcurrentConnections;
    private int retryMaxAttempts;
    private int retryBackoffMs;
    // Connection pool config
    private int poolMaxConnections;
    private int poolKeepAliveSeconds;
    private int poolIdleTimeoutSeconds;
    private final Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;

    public VirtualHost(UUID id, UUID agentId, String hostname, String targetHost,
                       int targetPort, VirtualHostProtocol protocol, VirtualHostStatus status,
                       UUID connectionId,
                       int circuitBreakerThreshold, int timeoutSeconds,
                       int maxConcurrentConnections, int retryMaxAttempts, int retryBackoffMs,
                       int poolMaxConnections, int poolKeepAliveSeconds, int poolIdleTimeoutSeconds,
                       Instant createdAt, String createdBy, Instant modifiedAt, String modifiedBy) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        this.agentId = Objects.requireNonNull(agentId, "Agent ID must not be null");
        setHostname(hostname);
        setTargetHost(targetHost);
        this.targetPort = targetPort;
        this.protocol = Objects.requireNonNull(protocol, "Protocol must not be null");
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.connectionId = connectionId;
        this.circuitBreakerThreshold = circuitBreakerThreshold;
        this.timeoutSeconds = timeoutSeconds;
        this.maxConcurrentConnections = maxConcurrentConnections;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryBackoffMs = retryBackoffMs;
        this.poolMaxConnections = poolMaxConnections;
        this.poolKeepAliveSeconds = poolKeepAliveSeconds;
        this.poolIdleTimeoutSeconds = poolIdleTimeoutSeconds;
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt must not be null");
        this.createdBy = createdBy;
        this.modifiedAt = modifiedAt;
        this.modifiedBy = modifiedBy;
    }

    public UUID getId() { return id; }

    public UUID getAgentId() { return agentId; }
    public void setAgentId(UUID agentId) { this.agentId = Objects.requireNonNull(agentId); }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) {
        Objects.requireNonNull(hostname, "Hostname must not be null");
        if (hostname.isBlank()) throw new IllegalArgumentException("Hostname must not be blank");
        this.hostname = hostname;
    }

    public String getTargetHost() { return targetHost; }
    public void setTargetHost(String targetHost) {
        Objects.requireNonNull(targetHost, "Target host must not be null");
        if (targetHost.isBlank()) throw new IllegalArgumentException("Target host must not be blank");
        this.targetHost = targetHost;
    }

    public int getTargetPort() { return targetPort; }
    public void setTargetPort(int targetPort) { this.targetPort = targetPort; }

    public VirtualHostProtocol getProtocol() { return protocol; }
    public void setProtocol(VirtualHostProtocol protocol) { this.protocol = Objects.requireNonNull(protocol); }

    public VirtualHostStatus getStatus() { return status; }
    public void setStatus(VirtualHostStatus status) { this.status = Objects.requireNonNull(status); }

    public UUID getConnectionId() { return connectionId; }
    public void setConnectionId(UUID connectionId) { this.connectionId = connectionId; }

    public int getCircuitBreakerThreshold() { return circuitBreakerThreshold; }
    public void setCircuitBreakerThreshold(int circuitBreakerThreshold) { this.circuitBreakerThreshold = circuitBreakerThreshold; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public int getMaxConcurrentConnections() { return maxConcurrentConnections; }
    public void setMaxConcurrentConnections(int maxConcurrentConnections) { this.maxConcurrentConnections = maxConcurrentConnections; }

    public int getRetryMaxAttempts() { return retryMaxAttempts; }
    public void setRetryMaxAttempts(int retryMaxAttempts) { this.retryMaxAttempts = retryMaxAttempts; }

    public int getRetryBackoffMs() { return retryBackoffMs; }
    public void setRetryBackoffMs(int retryBackoffMs) { this.retryBackoffMs = retryBackoffMs; }

    public int getPoolMaxConnections() { return poolMaxConnections; }
    public void setPoolMaxConnections(int poolMaxConnections) { this.poolMaxConnections = poolMaxConnections; }

    public int getPoolKeepAliveSeconds() { return poolKeepAliveSeconds; }
    public void setPoolKeepAliveSeconds(int poolKeepAliveSeconds) { this.poolKeepAliveSeconds = poolKeepAliveSeconds; }

    public int getPoolIdleTimeoutSeconds() { return poolIdleTimeoutSeconds; }
    public void setPoolIdleTimeoutSeconds(int poolIdleTimeoutSeconds) { this.poolIdleTimeoutSeconds = poolIdleTimeoutSeconds; }

    public Instant getCreatedAt() { return createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(Instant modifiedAt) { this.modifiedAt = modifiedAt; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
}
