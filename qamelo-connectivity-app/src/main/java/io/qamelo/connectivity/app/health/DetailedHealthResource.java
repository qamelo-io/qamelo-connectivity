package io.qamelo.connectivity.app.health;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.Map;

@Path("/api/v1/internal/connectivity/health")
@Produces(MediaType.APPLICATION_JSON)
public class DetailedHealthResource {

    @Inject
    Mutiny.SessionFactory sf;

    @GET
    public Uni<Map<String, Object>> health() {
        return sf.withSession(s ->
                        s.createNativeQuery("SELECT 1").getResultList()
                                .map(r -> Map.<String, Object>of(
                                        "status", "UP",
                                        "postgresql", Map.of("status", "CONNECTED"))))
                .onFailure().recoverWithItem(err ->
                        Map.of("status", "DOWN",
                                "postgresql", Map.of("status", "UNREACHABLE", "error", err.getMessage())));
    }
}
