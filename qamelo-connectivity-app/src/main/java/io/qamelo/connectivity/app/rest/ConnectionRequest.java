package io.qamelo.connectivity.app.rest;

import java.util.Map;
import java.util.UUID;

public record ConnectionRequest(
        String name,
        String type,
        String host,
        int port,
        String authType,
        String certManagement,
        UUID agentId,
        Map<String, String> properties,
        String description,
        String status
) {}
