package io.qamelo.connectivity.app.rest;

import java.util.UUID;

public record VirtualHostResponse(
        UUID id,
        UUID agentId,
        String hostname,
        String targetHost,
        int targetPort,
        String protocol,
        String status,
        UUID connectionId,
        int circuitBreakerThreshold,
        int timeoutSeconds,
        int maxConcurrentConnections,
        int retryMaxAttempts,
        int retryBackoffMs,
        int poolMaxConnections,
        int poolKeepAliveSeconds,
        int poolIdleTimeoutSeconds,
        String createdAt,
        String createdBy,
        String modifiedAt,
        String modifiedBy
) {}
