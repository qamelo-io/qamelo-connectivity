package io.qamelo.connectivity.app.rest;

import java.util.UUID;

public record AgentResponse(
        UUID id,
        String name,
        String description,
        String status,
        String registrationToken,
        String registrationTokenExpiresAt,
        String registeredAt,
        String certSerialNumber,
        String certExpiresAt,
        String certSubjectSan,
        String lastSeenAt,
        String tunnelRemoteAddress,
        String k8sNamespace,
        String createdAt,
        String createdBy,
        String modifiedAt,
        String modifiedBy
) {}
