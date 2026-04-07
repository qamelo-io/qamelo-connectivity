package io.qamelo.connectivity.app.rest;

import java.util.List;

public record AgentConfigResponse(
        AgentResponse agent,
        List<VirtualHostConfigEntry> virtualHosts,
        String gatewayUrl,
        String configVersion
) {
    public record VirtualHostConfigEntry(
            VirtualHostResponse virtualHost,
            String vaultCredentialPath
    ) {}
}
