package io.qamelo.connectivity.app.harness;

import io.qamelo.connectivity.domain.spi.GatewayClient;
import io.qamelo.connectivity.domain.spi.RoutingEntry;
import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory GatewayClient for integration tests.
 * Records push/remove calls for assertion in tests.
 */
@Mock
@ApplicationScoped
public class TestGatewayClient implements GatewayClient {

    private final List<List<RoutingEntry>> pushHistory = new CopyOnWriteArrayList<>();
    private final List<UUID> removeHistory = new CopyOnWriteArrayList<>();

    @Override
    public Uni<Void> pushRoutingTable(List<RoutingEntry> entries) {
        pushHistory.add(List.copyOf(entries));
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> removeRoutes(UUID agentId) {
        removeHistory.add(agentId);
        return Uni.createFrom().voidItem();
    }

    public List<List<RoutingEntry>> getPushHistory() { return pushHistory; }

    public List<UUID> getRemoveHistory() { return removeHistory; }

    public void reset() {
        pushHistory.clear();
        removeHistory.clear();
    }
}
