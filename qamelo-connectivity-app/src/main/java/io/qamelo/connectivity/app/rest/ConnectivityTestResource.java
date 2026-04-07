package io.qamelo.connectivity.app.rest;

import io.qamelo.connectivity.app.security.JwtAuthFilter;
import io.qamelo.connectivity.app.security.auth.AuthorizationService;
import io.qamelo.connectivity.domain.connection.ConnectionRepository;
import io.qamelo.connectivity.domain.connection.ConnectionStatus;
import io.qamelo.connectivity.domain.test.ConnectivityTest;
import io.qamelo.connectivity.domain.test.ConnectivityTestExecutor;
import io.qamelo.connectivity.domain.test.ConnectivityTestRepository;
import io.qamelo.connectivity.domain.test.TestDirection;
import io.qamelo.connectivity.domain.test.TestStatus;
import io.qamelo.connectivity.domain.test.TestType;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/connections/{connectionId}")
@Produces(MediaType.APPLICATION_JSON)
public class ConnectivityTestResource {

    private static final Logger LOG = Logger.getLogger(ConnectivityTestResource.class);

    @Inject
    ConnectionRepository connectionRepository;

    @Inject
    ConnectivityTestRepository testRepository;

    @Inject
    ConnectivityTestExecutor testExecutor;

    @Inject
    AuthorizationService authorizationService;

    @POST
    @Path("/test")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> triggerTest(@PathParam("connectionId") UUID connectionId,
                                     @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "connectivity-test:execute")
                .chain(() -> connectionRepository.findById(connectionId))
                .chain(connection -> {
                    if (connection == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Connection not found"))
                                        .build());
                    }

                    if (connection.getAgentId() != null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.BAD_REQUEST)
                                        .entity(Map.of("error", "bad_request",
                                                "message", "Agent-routed connections cannot be tested from cloud side"))
                                        .build());
                    }

                    Instant now = Instant.now();
                    ConnectivityTest test = new ConnectivityTest(
                            UUID.randomUUID(),
                            connectionId,
                            TestDirection.CLOUD_TO_REMOTE,
                            TestType.TCP_CONNECT,
                            TestStatus.PENDING,
                            null, null, null,
                            now, null,
                            userId
                    );

                    return testRepository.save(test)
                            .chain(saved -> testExecutor.executeTcpConnect(
                                    connection.getHost(), connection.getPort(), 10))
                            .chain(result -> {
                                test.setStatus(result.status());
                                test.setLatencyMs(result.latencyMs());
                                test.setResultMessage(result.resultMessage());
                                test.setErrorDetail(result.errorDetail());
                                test.setCompletedAt(Instant.now());

                                ConnectionStatus newStatus = result.status() == TestStatus.SUCCESS
                                        ? ConnectionStatus.ACTIVE
                                        : ConnectionStatus.ERROR;
                                connection.setStatus(newStatus);
                                connection.setModifiedAt(Instant.now());
                                connection.setModifiedBy(userId);

                                return testRepository.update(test)
                                        .chain(updatedTest -> connectionRepository.update(connection)
                                                .map(updatedConn -> Response.ok(toResponse(updatedTest)).build()));
                            });
                });
    }

    @GET
    @Path("/tests")
    public Uni<List<ConnectivityTestResponse>> getTestHistory(
            @PathParam("connectionId") UUID connectionId,
            @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "connection:read")
                .chain(() -> testRepository.findByConnectionId(connectionId)
                        .map(ConnectivityTestResource::toResponse)
                        .collect().asList());
    }

    static ConnectivityTestResponse toResponse(ConnectivityTest t) {
        return new ConnectivityTestResponse(
                t.getId(),
                t.getConnectionId(),
                t.getDirection().name(),
                t.getType().name(),
                t.getStatus().name(),
                t.getResultMessage(),
                t.getLatencyMs(),
                t.getErrorDetail(),
                t.getStartedAt() != null ? t.getStartedAt().toString() : null,
                t.getCompletedAt() != null ? t.getCompletedAt().toString() : null,
                t.getInitiatedBy()
        );
    }
}
