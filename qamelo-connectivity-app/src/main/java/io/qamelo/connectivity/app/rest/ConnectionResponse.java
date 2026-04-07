package io.qamelo.connectivity.app.rest;

import java.util.Map;
import java.util.UUID;

public record ConnectionResponse(
        UUID id,
        String name,
        String type,
        String host,
        int port,
        String authType,
        boolean hasCredentials,
        String certManagement,
        UUID agentId,
        Map<String, String> properties,
        String description,
        String status,
        String createdAt,
        String createdBy,
        String modifiedAt,
        String modifiedBy
) {}
