package io.qamelo.connectivity.app.rest;

public record VirtualHostRequest(
        String hostname,
        String targetHost,
        int targetPort,
        String protocol,
        String status,
        String connectionId,
        Integer circuitBreakerThreshold,
        Integer timeoutSeconds,
        Integer maxConcurrentConnections,
        Integer retryMaxAttempts,
        Integer retryBackoffMs,
        Integer poolMaxConnections,
        Integer poolKeepAliveSeconds,
        Integer poolIdleTimeoutSeconds
) {}
