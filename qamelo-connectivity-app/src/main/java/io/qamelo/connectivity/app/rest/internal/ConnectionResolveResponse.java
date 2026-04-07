package io.qamelo.connectivity.app.rest.internal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ConnectionResolveResponse(
        List<ConnectionResolutionEntry> resolved,
        List<String> notFound
) {

    public record ConnectionResolutionEntry(
            UUID id,
            String name,
            String type,
            String host,
            int port,
            String authType,
            String vaultCredentialPath,
            String certManagement,
            String vaultClientCertPath,
            String vaultTrustStorePath,
            UUID agentId,
            Map<String, String> properties,
            String status
    ) {}
}
