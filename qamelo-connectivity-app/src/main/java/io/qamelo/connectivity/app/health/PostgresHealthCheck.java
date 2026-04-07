package io.qamelo.connectivity.app.health;

import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.hibernate.reactive.mutiny.Mutiny;

@Readiness
@ApplicationScoped
public class PostgresHealthCheck implements AsyncHealthCheck {

    @Inject
    Mutiny.SessionFactory sf;

    @Override
    public Uni<HealthCheckResponse> call() {
        return sf.withSession(s ->
                        s.createNativeQuery("SELECT 1").getResultList()
                                .map(r -> HealthCheckResponse.named("PostgreSQL")
                                        .up()
                                        .withData("status", "CONNECTED")
                                        .build()))
                .onFailure().recoverWithItem(err ->
                        HealthCheckResponse.named("PostgreSQL")
                                .down()
                                .withData("status", "UNREACHABLE")
                                .withData("error", err.getMessage())
                                .build());
    }
}
