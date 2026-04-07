package io.qamelo.connectivity.app.rest;

import java.util.List;

public record AgentStatusReport(
        String tunnelState,
        String remoteAddress,
        List<HostHealthReport> hostHealth
) {
    public record HostHealthReport(String hostname, boolean reachable, Long latencyMs) {}
}
