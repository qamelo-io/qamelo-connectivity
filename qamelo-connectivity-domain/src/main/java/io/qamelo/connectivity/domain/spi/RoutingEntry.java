package io.qamelo.connectivity.domain.spi;

import java.util.UUID;

public record RoutingEntry(
        UUID agentId, String agentName, String agentCertSan,
        String virtualHostname, String targetHost, int targetPort,
        String protocol, UUID connectionId
) {}
