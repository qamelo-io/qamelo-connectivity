package io.qamelo.connectivity.app.rest.internal;

import io.qamelo.connectivity.app.rest.RoutingTableAssembler;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/api/v1/internal/routing-table")
@Produces(MediaType.APPLICATION_JSON)
public class InternalRoutingTableResource {

    @Inject
    RoutingTableAssembler routingTableAssembler;

    @GET
    public Uni<Response> getRoutingTable() {
        return routingTableAssembler.assembleFullTable()
                .map(entries -> Response.ok(Map.of("entries", entries)).build());
    }
}
