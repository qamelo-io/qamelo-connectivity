package io.qamelo.connectivity.infra.gateway;

import io.qamelo.connectivity.domain.spi.GatewayClient;
import io.qamelo.connectivity.domain.spi.RoutingEntry;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GatewayHttpClient implements GatewayClient {

    private static final Logger LOG = Logger.getLogger(GatewayHttpClient.class);

    private final WebClient webClient;
    private final String gatewayUrl;

    GatewayHttpClient(Vertx vertx,
                      @ConfigProperty(name = "qamelo.gateway.url",
                              defaultValue = "https://gateway.qamelo.io") String gatewayUrl) {
        this.webClient = WebClient.create(vertx);
        this.gatewayUrl = gatewayUrl;
    }

    @Override
    public Uni<Void> pushRoutingTable(List<RoutingEntry> entries) {
        String url = gatewayUrl + "/api/v1/internal/routing-table";
        JsonArray entriesArray = new JsonArray();
        for (RoutingEntry e : entries) {
            entriesArray.add(new JsonObject()
                    .put("agentId", e.agentId().toString())
                    .put("agentName", e.agentName())
                    .put("agentCertSan", e.agentCertSan())
                    .put("virtualHostname", e.virtualHostname())
                    .put("targetHost", e.targetHost())
                    .put("targetPort", e.targetPort())
                    .put("protocol", e.protocol())
                    .put("connectionId", e.connectionId() != null ? e.connectionId().toString() : null));
        }
        JsonObject body = new JsonObject().put("entries", entriesArray);
        LOG.debugf("Pushing routing table to gateway: %d entries", entries.size());

        return webClient.postAbs(url)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(body)
                .replaceWithVoid()
                .onFailure().recoverWithUni(ex -> {
                    LOG.warnf(ex, "Failed to push routing table to gateway, will retry on next reconciliation");
                    return Uni.createFrom().voidItem();
                });
    }

    @Override
    public Uni<Void> removeRoutes(UUID agentId) {
        String url = gatewayUrl + "/api/v1/internal/routing-table/agent/" + agentId;
        LOG.debugf("Removing routes for agent %s from gateway", agentId);

        return webClient.deleteAbs(url)
                .send()
                .replaceWithVoid()
                .onFailure().recoverWithUni(ex -> {
                    LOG.warnf(ex, "Failed to remove routes for agent %s, will be cleaned up by reconciler", agentId);
                    return Uni.createFrom().voidItem();
                });
    }
}
