package io.qamelo.connectivity.app.rest;

import io.qamelo.connectivity.app.security.JwtAuthFilter;
import io.qamelo.connectivity.app.security.auth.AuthorizationService;
import io.qamelo.connectivity.domain.agent.AgentRepository;
import io.qamelo.connectivity.domain.agent.KubernetesServiceManager;
import io.qamelo.connectivity.domain.agent.VirtualHost;
import io.qamelo.connectivity.domain.agent.VirtualHostProtocol;
import io.qamelo.connectivity.domain.agent.VirtualHostRepository;
import io.qamelo.connectivity.domain.agent.VirtualHostStatus;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/agents/{agentId}/virtual-hosts")
@Produces(MediaType.APPLICATION_JSON)
public class VirtualHostResource {

    private static final Logger LOG = Logger.getLogger(VirtualHostResource.class);

    @Inject
    VirtualHostRepository virtualHostRepository;

    @Inject
    AgentRepository agentRepository;

    @Inject
    AuthorizationService authorizationService;

    @Inject
    KubernetesServiceManager kubernetesServiceManager;

    @ConfigProperty(name = "qamelo.k8s.runtime-namespace", defaultValue = "qamelo-runtime")
    String runtimeNamespace;

    @ConfigProperty(name = "qamelo.k8s.gateway-ip", defaultValue = "10.96.0.100")
    String gatewayIp;

    @GET
    public Uni<Response> list(@PathParam("agentId") UUID agentId,
                              @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "agent:read")
                .chain(() -> agentRepository.findById(agentId))
                .chain(agent -> {
                    if (agent == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Agent not found"))
                                        .build());
                    }
                    return virtualHostRepository.findByAgentId(agentId)
                            .map(vhs -> {
                                List<VirtualHostResponse> responses = vhs.stream()
                                        .map(VirtualHostResource::toResponse)
                                        .toList();
                                return Response.ok(responses).build();
                            });
                });
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> create(@PathParam("agentId") UUID agentId, VirtualHostRequest request,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        LOG.infof("Creating virtual host: hostname=%s for agent=%s", request.hostname(), agentId);
        return authorizationService.requirePermission(userId, "agent:manage")
                .chain(() -> agentRepository.findById(agentId))
                .chain(agent -> {
                    if (agent == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Agent not found"))
                                        .build());
                    }
                    Instant now = Instant.now();
                    UUID vhId = UUID.randomUUID();
                    String namespace = agent.getK8sNamespace() != null ? agent.getK8sNamespace() : runtimeNamespace;
                    int targetPort = request.targetPort();

                    VirtualHost vh = new VirtualHost(
                            vhId,
                            agentId,
                            request.hostname(),
                            request.targetHost(),
                            targetPort,
                            request.protocol() != null ? VirtualHostProtocol.valueOf(request.protocol()) : VirtualHostProtocol.TCP,
                            request.status() != null ? VirtualHostStatus.valueOf(request.status()) : VirtualHostStatus.ACTIVE,
                            request.connectionId() != null ? UUID.fromString(request.connectionId()) : null,
                            request.circuitBreakerThreshold() != null ? request.circuitBreakerThreshold() : 5,
                            request.timeoutSeconds() != null ? request.timeoutSeconds() : 30,
                            request.maxConcurrentConnections() != null ? request.maxConcurrentConnections() : 100,
                            request.retryMaxAttempts() != null ? request.retryMaxAttempts() : 3,
                            request.retryBackoffMs() != null ? request.retryBackoffMs() : 1000,
                            request.poolMaxConnections() != null ? request.poolMaxConnections() : 10,
                            request.poolKeepAliveSeconds() != null ? request.poolKeepAliveSeconds() : 60,
                            request.poolIdleTimeoutSeconds() != null ? request.poolIdleTimeoutSeconds() : 300,
                            now,
                            userId,
                            now,
                            userId
                    );

                    // 1) Create K8s Service first; if it fails -> 503
                    return kubernetesServiceManager.createService(namespace, vh.getHostname(),
                                    agentId, vhId, gatewayIp, targetPort)
                            .chain(() -> virtualHostRepository.save(vh))
                            .map(saved -> Response.status(Response.Status.CREATED)
                                    .entity(toResponse(saved)).build())
                            .onFailure().recoverWithItem(ex -> {
                                LOG.errorf(ex, "Failed to create K8s service or save virtual host %s", request.hostname());
                                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                                        .entity(Map.of("error", "service_unavailable",
                                                "message", "Failed to create K8s service: " + ex.getMessage()))
                                        .build();
                            });
                })
                .onFailure(IllegalArgumentException.class)
                .recoverWithItem(ex -> Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "bad_request", "message", ex.getMessage())).build());
    }

    @PUT
    @Path("/{vhId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> update(@PathParam("agentId") UUID agentId,
                                @PathParam("vhId") UUID vhId,
                                VirtualHostRequest request,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "agent:manage")
                .chain(() -> virtualHostRepository.findById(vhId))
                .chain(existing -> {
                    if (existing == null || !existing.getAgentId().equals(agentId)) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Virtual host not found"))
                                        .build());
                    }
                    // Update resiliency + pool config
                    if (request.circuitBreakerThreshold() != null) {
                        existing.setCircuitBreakerThreshold(request.circuitBreakerThreshold());
                    }
                    if (request.timeoutSeconds() != null) {
                        existing.setTimeoutSeconds(request.timeoutSeconds());
                    }
                    if (request.maxConcurrentConnections() != null) {
                        existing.setMaxConcurrentConnections(request.maxConcurrentConnections());
                    }
                    if (request.retryMaxAttempts() != null) {
                        existing.setRetryMaxAttempts(request.retryMaxAttempts());
                    }
                    if (request.retryBackoffMs() != null) {
                        existing.setRetryBackoffMs(request.retryBackoffMs());
                    }
                    if (request.poolMaxConnections() != null) {
                        existing.setPoolMaxConnections(request.poolMaxConnections());
                    }
                    if (request.poolKeepAliveSeconds() != null) {
                        existing.setPoolKeepAliveSeconds(request.poolKeepAliveSeconds());
                    }
                    if (request.poolIdleTimeoutSeconds() != null) {
                        existing.setPoolIdleTimeoutSeconds(request.poolIdleTimeoutSeconds());
                    }
                    if (request.status() != null) {
                        existing.setStatus(VirtualHostStatus.valueOf(request.status()));
                    }
                    existing.setModifiedAt(Instant.now());
                    existing.setModifiedBy(userId);
                    return virtualHostRepository.update(existing)
                            .map(updated -> Response.ok(toResponse(updated)).build());
                })
                .onFailure(IllegalArgumentException.class)
                .recoverWithItem(ex -> Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "bad_request", "message", ex.getMessage())).build());
    }

    @DELETE
    @Path("/{vhId}")
    public Uni<Response> delete(@PathParam("agentId") UUID agentId,
                                @PathParam("vhId") UUID vhId,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "agent:manage")
                .chain(() -> virtualHostRepository.findById(vhId))
                .chain(existing -> {
                    if (existing == null || !existing.getAgentId().equals(agentId)) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Virtual host not found"))
                                        .build());
                    }
                    return agentRepository.findById(agentId)
                            .chain(agent -> {
                                String namespace = (agent != null && agent.getK8sNamespace() != null)
                                        ? agent.getK8sNamespace() : runtimeNamespace;
                                // 1) Delete from DB first, 2) Best-effort K8s cleanup
                                return virtualHostRepository.delete(vhId)
                                        .chain(() -> kubernetesServiceManager.deleteService(namespace, existing.getHostname())
                                                .onFailure().recoverWithUni(ex -> {
                                                    LOG.warnf(ex, "Failed to delete K8s service %s for virtual host %s, proceeding",
                                                            existing.getHostname(), vhId);
                                                    return Uni.createFrom().voidItem();
                                                }))
                                        .map(v -> Response.noContent().build());
                            });
                });
    }

    static VirtualHostResponse toResponse(VirtualHost vh) {
        return new VirtualHostResponse(
                vh.getId(),
                vh.getAgentId(),
                vh.getHostname(),
                vh.getTargetHost(),
                vh.getTargetPort(),
                vh.getProtocol().name(),
                vh.getStatus().name(),
                vh.getConnectionId(),
                vh.getCircuitBreakerThreshold(),
                vh.getTimeoutSeconds(),
                vh.getMaxConcurrentConnections(),
                vh.getRetryMaxAttempts(),
                vh.getRetryBackoffMs(),
                vh.getPoolMaxConnections(),
                vh.getPoolKeepAliveSeconds(),
                vh.getPoolIdleTimeoutSeconds(),
                vh.getCreatedAt() != null ? vh.getCreatedAt().toString() : null,
                vh.getCreatedBy(),
                vh.getModifiedAt() != null ? vh.getModifiedAt().toString() : null,
                vh.getModifiedBy()
        );
    }
}
