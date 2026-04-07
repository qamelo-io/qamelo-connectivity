package io.qamelo.connectivity.domain.spi;

import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

/**
 * SPI for pushing routing table updates to the gateway reverse-proxy.
 * Implementations live in the infra module.
 */
public interface GatewayClient {

    Uni<Void> pushRoutingTable(List<RoutingEntry> entries);

    Uni<Void> removeRoutes(UUID agentId);
}
