package io.qamelo.connectivity.app.rest;

import java.util.UUID;

public record CertificateResponse(
        UUID id,
        String name,
        UUID partnerId,
        UUID connectionId,
        UUID agentId,
        String usage,
        String source,
        String vaultPath,
        String serialNumber,
        String subjectDn,
        String issuerDn,
        String notBefore,
        String notAfter,
        String status,
        int version,
        String createdAt,
        String createdBy,
        String modifiedAt,
        String modifiedBy
) {}
