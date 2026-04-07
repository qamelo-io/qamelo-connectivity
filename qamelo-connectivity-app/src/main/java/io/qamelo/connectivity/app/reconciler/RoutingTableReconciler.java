package io.qamelo.connectivity.app.reconciler;

import io.qamelo.connectivity.app.rest.RoutingTableAssembler;
import io.qamelo.connectivity.domain.spi.GatewayClient;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RoutingTableReconciler {

    private static final Logger LOG = Logger.getLogger(RoutingTableReconciler.class);

    @Inject
    RoutingTableAssembler routingTableAssembler;

    @Inject
    GatewayClient gatewayClient;

    void onStart(@Observes StartupEvent ev) {
        pushFullTable().subscribe().with(
                v -> LOG.info("Startup routing table push complete"),
                ex -> LOG.warnf(ex, "Failed to push routing table on startup")
        );
    }

    @io.quarkus.scheduler.Scheduled(every = "60s")
    Uni<Void> reconcile() {
        return pushFullTable();
    }

    Uni<Void> pushFullTable() {
        return routingTableAssembler.assembleFullTable()
                .chain(entries -> gatewayClient.pushRoutingTable(entries));
    }
}
