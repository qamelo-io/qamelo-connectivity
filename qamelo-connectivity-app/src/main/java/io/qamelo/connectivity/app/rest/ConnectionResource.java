package io.qamelo.connectivity.app.rest;

import io.qamelo.connectivity.app.security.JwtAuthFilter;
import io.qamelo.connectivity.app.security.auth.AuthorizationService;
import io.qamelo.connectivity.domain.connection.AuthType;
import io.qamelo.connectivity.domain.connection.CertManagement;
import io.qamelo.connectivity.domain.connection.Connection;
import io.qamelo.connectivity.domain.connection.ConnectionRepository;
import io.qamelo.connectivity.domain.connection.ConnectionStatus;
import io.qamelo.connectivity.domain.connection.ConnectionType;
import io.qamelo.connectivity.domain.spi.SecretsClient;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
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

@Path("/api/v1/connections")
@Produces(MediaType.APPLICATION_JSON)
public class ConnectionResource {

    private static final Logger LOG = Logger.getLogger(ConnectionResource.class);

    @Inject
    ConnectionRepository connectionRepository;

    @Inject
    AuthorizationService authorizationService;

    @Inject
    SecretsClient secretsClient;

    @GET
    public Uni<List<ConnectionResponse>> list(@Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "connection:read")
                .chain(() -> connectionRepository.findAll()
                        .map(ConnectionResource::toResponse)
                        .collect().asList());
    }

    @GET
    @Path("/{id}")
    public Uni<Response> get(@PathParam("id") UUID id,
                             @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "connection:read")
                .chain(() -> connectionRepository.findById(id))
                .map(connection -> {
                    if (connection == null) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "not_found", "message", "Connection not found"))
                                .build();
                    }
                    return Response.ok(toResponse(connection)).build();
                });
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> create(ConnectionRequest request,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        LOG.infof("Creating connection: name=%s, type=%s", request.name(), request.type());
        return authorizationService.requirePermission(userId, "connection:manage")
                .chain(() -> {
                    Connection connection = fromRequest(request, userId);
                    if (hasCredentials(request)) {
                        String vaultPath = vaultPath(connection.getId());
                        return secretsClient.writeCredential(vaultPath, request.credentials())
                                .invoke(() -> connection.setVaultCredentialPath(vaultPath))
                                .chain(() -> connectionRepository.save(connection));
                    }
                    return connectionRepository.save(connection);
                })
                .map(saved -> Response.status(Response.Status.CREATED)
                        .entity(toResponse(saved)).build())
                .onFailure(IllegalArgumentException.class)
                .recoverWithItem(ex -> Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "bad_request", "message", ex.getMessage())).build());
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> update(@PathParam("id") UUID id, ConnectionRequest request,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "connection:manage")
                .chain(() -> connectionRepository.findById(id))
                .chain(existing -> {
                    if (existing == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Connection not found"))
                                        .build());
                    }
                    applyUpdate(existing, request, userId);
                    return handleCredentialUpdate(existing, request)
                            .chain(() -> connectionRepository.update(existing))
                            .map(updated -> Response.ok(toResponse(updated)).build());
                })
                .onFailure(IllegalArgumentException.class)
                .recoverWithItem(ex -> Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "bad_request", "message", ex.getMessage())).build());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> delete(@PathParam("id") UUID id,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "connection:manage")
                .chain(() -> connectionRepository.findById(id))
                .chain(existing -> {
                    if (existing == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Connection not found"))
                                        .build());
                    }
                    Uni<Void> vaultCleanup = Uni.createFrom().voidItem();
                    if (existing.getVaultCredentialPath() != null) {
                        vaultCleanup = secretsClient.deleteCredential(existing.getVaultCredentialPath())
                                .onFailure().recoverWithUni(ex -> {
                                    LOG.warnf(ex, "Failed to delete Vault credential at %s for connection %s, proceeding with DB delete",
                                            existing.getVaultCredentialPath(), id);
                                    return Uni.createFrom().voidItem();
                                });
                    }
                    return vaultCleanup
                            .chain(() -> connectionRepository.delete(id))
                            .map(v -> Response.noContent().build());
                });
    }

    /**
     * Handle credential update semantics:
     * - credentials non-null + non-empty -> overwrite in Vault
     * - credentials null -> leave unchanged
     * - credentials non-null + empty map -> delete from Vault, clear path
     */
    private Uni<Void> handleCredentialUpdate(Connection existing, ConnectionRequest request) {
        if (request.credentials() == null) {
            // Credentials absent — leave unchanged
            return Uni.createFrom().voidItem();
        }
        if (request.credentials().isEmpty()) {
            // Empty map — delete from Vault and clear path
            if (existing.getVaultCredentialPath() != null) {
                String pathToDelete = existing.getVaultCredentialPath();
                existing.setVaultCredentialPath(null);
                return secretsClient.deleteCredential(pathToDelete)
                        .onFailure().recoverWithUni(ex -> {
                            LOG.warnf(ex, "Failed to delete Vault credential at %s during update, path cleared anyway",
                                    pathToDelete);
                            return Uni.createFrom().voidItem();
                        });
            }
            existing.setVaultCredentialPath(null);
            return Uni.createFrom().voidItem();
        }
        // Non-empty credentials — overwrite in Vault
        String vaultPath = existing.getVaultCredentialPath() != null
                ? existing.getVaultCredentialPath()
                : vaultPath(existing.getId());
        return secretsClient.writeCredential(vaultPath, request.credentials())
                .invoke(() -> existing.setVaultCredentialPath(vaultPath));
    }

    private Connection fromRequest(ConnectionRequest request, String userId) {
        Instant now = Instant.now();
        return new Connection(
                UUID.randomUUID(),
                request.name(),
                ConnectionType.valueOf(request.type()),
                request.host(),
                request.port(),
                AuthType.valueOf(request.authType()),
                null, // vaultCredentialPath — set after Vault write
                request.certManagement() != null ? CertManagement.valueOf(request.certManagement()) : null,
                null, // vaultClientCertPath — future phase
                null, // vaultTrustStorePath — future phase
                request.agentId(),
                request.properties(),
                request.description(),
                request.status() != null ? ConnectionStatus.valueOf(request.status()) : ConnectionStatus.ACTIVE,
                now,
                userId,
                now,
                userId
        );
    }

    private void applyUpdate(Connection existing, ConnectionRequest request, String userId) {
        existing.setName(request.name());
        existing.setType(ConnectionType.valueOf(request.type()));
        existing.setHost(request.host());
        existing.setPort(request.port());
        existing.setAuthType(AuthType.valueOf(request.authType()));
        existing.setCertManagement(request.certManagement() != null
                ? CertManagement.valueOf(request.certManagement()) : null);
        existing.setAgentId(request.agentId());
        existing.setProperties(request.properties());
        existing.setDescription(request.description());
        existing.setStatus(request.status() != null
                ? ConnectionStatus.valueOf(request.status()) : existing.getStatus());
        existing.setModifiedAt(Instant.now());
        existing.setModifiedBy(userId);
    }

    static ConnectionResponse toResponse(Connection c) {
        return new ConnectionResponse(
                c.getId(),
                c.getName(),
                c.getType().name(),
                c.getHost(),
                c.getPort(),
                c.getAuthType().name(),
                c.getVaultCredentialPath() != null,
                c.getCertManagement() != null ? c.getCertManagement().name() : null,
                c.getAgentId(),
                c.getProperties(),
                c.getDescription(),
                c.getStatus().name(),
                c.getCreatedAt() != null ? c.getCreatedAt().toString() : null,
                c.getCreatedBy(),
                c.getModifiedAt() != null ? c.getModifiedAt().toString() : null,
                c.getModifiedBy()
        );
    }

    private static boolean hasCredentials(ConnectionRequest request) {
        return request.credentials() != null && !request.credentials().isEmpty();
    }

    private static String vaultPath(UUID connectionId) {
        return "connectivity/connections/" + connectionId + "/credentials";
    }
}
