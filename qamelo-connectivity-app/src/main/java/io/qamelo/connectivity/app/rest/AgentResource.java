package io.qamelo.connectivity.app.rest;

import io.qamelo.connectivity.app.security.JwtAuthFilter;
import io.qamelo.connectivity.app.security.auth.AuthorizationService;
import io.qamelo.connectivity.domain.agent.Agent;
import io.qamelo.connectivity.domain.agent.AgentRepository;
import io.qamelo.connectivity.domain.agent.AgentStatus;
import io.qamelo.connectivity.domain.agent.VirtualHostRepository;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/agents")
@Produces(MediaType.APPLICATION_JSON)
public class AgentResource {

    private static final Logger LOG = Logger.getLogger(AgentResource.class);
    private static final String PKI_MOUNT = "pki-tunnel";
    private static final String PKI_ROLE = "role-agent";
    private static final String CERT_TTL = "2160h"; // 90 days
    private static final String SAN_SUFFIX = ".agents.qamelo.io";

    @Inject
    AgentRepository agentRepository;

    @Inject
    AuthorizationService authorizationService;

    @Inject
    SecretsClient secretsClient;

    @Inject
    VirtualHostRepository virtualHostRepository;

    @ConfigProperty(name = "qamelo.gateway.url", defaultValue = "https://gateway.qamelo.io")
    String gatewayUrl;

    // --- Admin CRUD endpoints (JWT auth, agent:read / agent:manage) ---

    @GET
    public Uni<List<AgentResponse>> list(@Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "agent:read")
                .chain(() -> agentRepository.findAll()
                        .map(AgentResource::toResponse)
                        .collect().asList());
    }

    @GET
    @Path("/{id}")
    public Uni<Response> get(@PathParam("id") UUID id,
                             @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "agent:read")
                .chain(() -> agentRepository.findById(id))
                .map(agent -> {
                    if (agent == null) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "not_found", "message", "Agent not found"))
                                .build();
                    }
                    return Response.ok(toResponse(agent)).build();
                });
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> create(AgentRequest request,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        LOG.infof("Creating agent: name=%s", request.name());
        return authorizationService.requirePermission(userId, "agent:manage")
                .chain(() -> {
                    Instant now = Instant.now();
                    String token = generateRegistrationToken();
                    Agent agent = new Agent(
                            UUID.randomUUID(),
                            request.name(),
                            request.description(),
                            AgentStatus.PENDING,
                            token,
                            now.plus(1, ChronoUnit.HOURS),
                            null, // registeredAt
                            null, // certSerialNumber
                            null, // certExpiresAt
                            null, // certSubjectSan
                            null, // lastSeenAt
                            null, // tunnelRemoteAddress
                            request.k8sNamespace(),
                            now,
                            userId,
                            now,
                            userId
                    );
                    return agentRepository.save(agent);
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
    public Uni<Response> update(@PathParam("id") UUID id, AgentRequest request,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "agent:manage")
                .chain(() -> agentRepository.findById(id))
                .chain(existing -> {
                    if (existing == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Agent not found"))
                                        .build());
                    }
                    existing.setName(request.name());
                    existing.setDescription(request.description());
                    existing.setK8sNamespace(request.k8sNamespace());
                    existing.setModifiedAt(Instant.now());
                    existing.setModifiedBy(userId);
                    return agentRepository.update(existing)
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
        return authorizationService.requirePermission(userId, "agent:manage")
                .chain(() -> agentRepository.findById(id))
                .chain(existing -> {
                    if (existing == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Agent not found"))
                                        .build());
                    }
                    return agentRepository.hasVirtualHosts(id)
                            .chain(hasVirtualHosts -> {
                                if (hasVirtualHosts) {
                                    return Uni.createFrom().item(
                                            Response.status(Response.Status.CONFLICT)
                                                    .entity(Map.of("error", "conflict",
                                                            "message", "Cannot delete agent with active virtual hosts"))
                                                    .build());
                                }
                                // Best-effort cert revocation for registered/connected agents
                                Uni<Void> revoke = Uni.createFrom().voidItem();
                                if ((existing.getStatus() == AgentStatus.REGISTERED
                                        || existing.getStatus() == AgentStatus.CONNECTED)
                                        && existing.getCertSerialNumber() != null) {
                                    revoke = secretsClient.revokeCertificate(PKI_MOUNT, existing.getCertSerialNumber())
                                            .onFailure().recoverWithUni(ex -> {
                                                LOG.warnf(ex, "Failed to revoke certificate %s for agent %s, proceeding with delete",
                                                        existing.getCertSerialNumber(), id);
                                                return Uni.createFrom().voidItem();
                                            });
                                }
                                return revoke
                                        .chain(() -> agentRepository.delete(id))
                                        .map(v -> Response.noContent().build());
                            });
                });
    }

    @POST
    @Path("/{id}/regenerate-token")
    public Uni<Response> regenerateToken(@PathParam("id") UUID id,
                                         @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "agent:manage")
                .chain(() -> agentRepository.findById(id))
                .chain(existing -> {
                    if (existing == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Agent not found"))
                                        .build());
                    }
                    if (existing.getStatus() != AgentStatus.PENDING) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.BAD_REQUEST)
                                        .entity(Map.of("error", "bad_request",
                                                "message", "Can only regenerate token for PENDING agents"))
                                        .build());
                    }
                    existing.setRegistrationToken(generateRegistrationToken());
                    existing.setRegistrationTokenExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
                    existing.setModifiedAt(Instant.now());
                    existing.setModifiedBy(userId);
                    return agentRepository.update(existing)
                            .map(updated -> Response.ok(toResponse(updated)).build());
                });
    }

    // --- Registration endpoint (uses one-time token, NOT JWT auth) ---

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> register(RegistrationRequest request) {
        LOG.infof("Agent registration attempt");
        return agentRepository.findByRegistrationToken(request.token())
                .chain(agent -> {
                    if (agent == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.UNAUTHORIZED)
                                        .entity(Map.of("error", "unauthorized", "message", "Invalid registration token"))
                                        .build());
                    }
                    if (agent.getRegistrationTokenExpiresAt() != null
                            && agent.getRegistrationTokenExpiresAt().isBefore(Instant.now())) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.UNAUTHORIZED)
                                        .entity(Map.of("error", "unauthorized", "message", "Registration token expired"))
                                        .build());
                    }
                    String agentSan = agent.getId() + SAN_SUFFIX;
                    return secretsClient.signCsr(PKI_MOUNT, PKI_ROLE, request.csr(),
                                    agentSan, agentSan, CERT_TTL)
                            .chain(pkiResponse -> {
                                agent.setStatus(AgentStatus.REGISTERED);
                                agent.setRegistrationToken(null);
                                agent.setRegistrationTokenExpiresAt(null);
                                agent.setRegisteredAt(Instant.now());
                                agent.setCertSerialNumber(pkiResponse.serialNumber());
                                agent.setCertExpiresAt(pkiResponse.expiration());
                                agent.setCertSubjectSan(agentSan);
                                agent.setModifiedAt(Instant.now());
                                return agentRepository.update(agent)
                                        .map(updated -> Response.ok(
                                                new RegistrationResponse(
                                                        pkiResponse.certificate(),
                                                        pkiResponse.caChain(),
                                                        gatewayUrl)
                                        ).build());
                            });
                });
    }

    // --- Certificate renewal endpoint ---
    // TODO: In production this would use AgentCertFilter (mTLS). For now uses JWT auth.

    @POST
    @Path("/{id}/renew-cert")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> renewCert(@PathParam("id") UUID id, RenewalRequest request,
                                   @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "agent:manage")
                .chain(() -> agentRepository.findById(id))
                .chain(agent -> {
                    if (agent == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Agent not found"))
                                        .build());
                    }
                    if (agent.getStatus() != AgentStatus.REGISTERED
                            && agent.getStatus() != AgentStatus.CONNECTED) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.BAD_REQUEST)
                                        .entity(Map.of("error", "bad_request",
                                                "message", "Agent must be REGISTERED or CONNECTED to renew certificate"))
                                        .build());
                    }
                    String agentSan = agent.getId() + SAN_SUFFIX;
                    return secretsClient.signCsr(PKI_MOUNT, PKI_ROLE, request.csr(),
                                    agentSan, agentSan, CERT_TTL)
                            .chain(pkiResponse -> {
                                agent.setCertSerialNumber(pkiResponse.serialNumber());
                                agent.setCertExpiresAt(pkiResponse.expiration());
                                agent.setModifiedAt(Instant.now());
                                return agentRepository.update(agent)
                                        .map(updated -> Response.ok(
                                                new RenewalResponse(
                                                        pkiResponse.certificate(),
                                                        pkiResponse.caChain())
                                        ).build());
                            });
                });
    }

    // --- Agent config endpoint ---

    @GET
    @Path("/{id}/config")
    public Uni<Response> config(@PathParam("id") UUID id,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "agent:read")
                .chain(() -> agentRepository.findById(id))
                .chain(agent -> {
                    if (agent == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Agent not found"))
                                        .build());
                    }
                    return virtualHostRepository.findByAgentId(id)
                            .map(vhs -> {
                                List<VirtualHostResponse> vhResponses = vhs.stream()
                                        .map(VirtualHostResource::toResponse)
                                        .toList();
                                return Response.ok(Map.of(
                                        "agent", toResponse(agent),
                                        "virtualHosts", vhResponses
                                )).build();
                            });
                });
    }

    // --- Agent status report endpoint ---
    // TODO: In production this would use AgentCertFilter (mTLS). For now uses JWT auth.

    @POST
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> statusReport(@PathParam("id") UUID id, AgentStatusReport report,
                                      @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "agent:manage")
                .chain(() -> agentRepository.findById(id))
                .chain(agent -> {
                    if (agent == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Agent not found"))
                                        .build());
                    }
                    agent.setLastSeenAt(Instant.now());
                    agent.setTunnelRemoteAddress(report.remoteAddress());
                    if (agent.getStatus() == AgentStatus.REGISTERED) {
                        agent.setStatus(AgentStatus.CONNECTED);
                    }
                    agent.setModifiedAt(Instant.now());
                    return agentRepository.update(agent)
                            .map(updated -> Response.ok(toResponse(updated)).build());
                });
    }

    // --- Helper methods ---

    static AgentResponse toResponse(Agent a) {
        return new AgentResponse(
                a.getId(),
                a.getName(),
                a.getDescription(),
                a.getStatus().name(),
                // Only expose registration token for PENDING agents
                a.getStatus() == AgentStatus.PENDING ? a.getRegistrationToken() : null,
                a.getRegistrationTokenExpiresAt() != null ? a.getRegistrationTokenExpiresAt().toString() : null,
                a.getRegisteredAt() != null ? a.getRegisteredAt().toString() : null,
                a.getCertSerialNumber(),
                a.getCertExpiresAt() != null ? a.getCertExpiresAt().toString() : null,
                a.getCertSubjectSan(),
                a.getLastSeenAt() != null ? a.getLastSeenAt().toString() : null,
                a.getTunnelRemoteAddress(),
                a.getK8sNamespace(),
                a.getCreatedAt() != null ? a.getCreatedAt().toString() : null,
                a.getCreatedBy(),
                a.getModifiedAt() != null ? a.getModifiedAt().toString() : null,
                a.getModifiedBy()
        );
    }

    private static String generateRegistrationToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
