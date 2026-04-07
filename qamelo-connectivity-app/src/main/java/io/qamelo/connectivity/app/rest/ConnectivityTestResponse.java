package io.qamelo.connectivity.app.rest;

import java.util.UUID;

public record ConnectivityTestResponse(
        UUID id,
        UUID connectionId,
        String direction,
        String type,
        String status,
        String resultMessage,
        Long latencyMs,
        String errorDetail,
        String startedAt,
        String completedAt,
        String initiatedBy
) {}
