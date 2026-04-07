package io.qamelo.connectivity.app.rest;

import io.qamelo.connectivity.app.security.JwtAuthFilter;
import io.qamelo.connectivity.app.security.auth.AuthorizationService;
import io.qamelo.connectivity.domain.agreement.Agreement;
import io.qamelo.connectivity.domain.agreement.AgreementDirection;
import io.qamelo.connectivity.domain.agreement.AgreementRepository;
import io.qamelo.connectivity.domain.agreement.AgreementStatus;
import io.qamelo.connectivity.domain.agreement.RetryPolicy;
import io.qamelo.connectivity.domain.channel.ChannelRepository;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/agreements")
@Produces(MediaType.APPLICATION_JSON)
public class AgreementResource {

    private static final Logger LOG = Logger.getLogger(AgreementResource.class);

    @Inject
    AgreementRepository agreementRepository;

    @Inject
    ChannelRepository channelRepository;

    @Inject
    AuthorizationService authorizationService;

    @GET
    public Uni<List<AgreementResponse>> list(@QueryParam("channelId") String channelId,
                                             @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "agreement:read")
                .chain(() -> {
                    if (channelId != null && !channelId.isBlank()) {
                        return agreementRepository.findByChannelId(UUID.fromString(channelId))
                                .map(AgreementResource::toResponse)
                                .collect().asList();
                    }
                    return agreementRepository.findAll()
                            .map(AgreementResource::toResponse)
                            .collect().asList();
                });
    }

    @GET
    @Path("/{id}")
    public Uni<Response> get(@PathParam("id") UUID id,
                             @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "agreement:read")
                .chain(() -> agreementRepository.findById(id))
                .map(agreement -> {
                    if (agreement == null) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "not_found", "message", "Agreement not found"))
                                .build();
                    }
                    return Response.ok(toResponse(agreement)).build();
                });
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> create(AgreementRequest request,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        LOG.infof("Creating agreement: name=%s, documentType=%s", request.name(), request.documentType());
        return authorizationService.requirePermission(userId, "agreement:manage")
                .chain(() -> {
                    UUID channelId = UUID.fromString(request.channelId());
                    return channelRepository.findById(channelId)
                            .chain(channel -> {
                                if (channel == null) {
                                    return Uni.createFrom().item(
                                            Response.status(Response.Status.BAD_REQUEST)
                                                    .entity(Map.of("error", "bad_request",
                                                            "message", "Channel not found: " + channelId))
                                                    .build());
                                }
                                Agreement agreement = fromRequest(request, userId);
                                return agreementRepository.save(agreement)
                                        .map(saved -> Response.status(Response.Status.CREATED)
                                                .entity(toResponse(saved)).build());
                            });
                })
                .onFailure(IllegalArgumentException.class)
                .recoverWithItem(ex -> Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "bad_request", "message", ex.getMessage())).build());
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> update(@PathParam("id") UUID id, AgreementRequest request,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "agreement:manage")
                .chain(() -> agreementRepository.findById(id))
                .chain(existing -> {
                    if (existing == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Agreement not found"))
                                        .build());
                    }
                    // Check optimistic locking: request version must match DB version
                    if (request.version() != null && request.version() != existing.getVersion()) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.CONFLICT)
                                        .entity(Map.of("error", "conflict",
                                                "message", "Version conflict: expected " + existing.getVersion()
                                                        + " but got " + request.version()))
                                        .build());
                    }
                    // Validate status transition if status is changing
                    if (request.status() != null) {
                        AgreementStatus newStatus = AgreementStatus.valueOf(request.status());
                        if (newStatus != existing.getStatus()) {
                            try {
                                existing.transitionTo(newStatus);
                            } catch (IllegalArgumentException e) {
                                return Uni.createFrom().item(
                                        Response.status(Response.Status.BAD_REQUEST)
                                                .entity(Map.of("error", "bad_request", "message", e.getMessage()))
                                                .build());
                            }
                        }
                    }
                    applyUpdate(existing, request, userId);
                    return agreementRepository.update(existing)
                            .map(updated -> Response.ok(toResponse(updated)).build());
                })
                .onFailure(IllegalArgumentException.class)
                .recoverWithItem(ex -> Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "bad_request", "message", ex.getMessage())).build())
                .onFailure(jakarta.persistence.OptimisticLockException.class)
                .recoverWithItem(ex -> Response.status(Response.Status.CONFLICT)
                        .entity(Map.of("error", "conflict", "message", "Concurrent modification detected")).build())
                .onFailure(org.hibernate.StaleObjectStateException.class)
                .recoverWithItem(ex -> Response.status(Response.Status.CONFLICT)
                        .entity(Map.of("error", "conflict", "message", "Concurrent modification detected")).build());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> delete(@PathParam("id") UUID id,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "agreement:manage")
                .chain(() -> agreementRepository.findById(id))
                .chain(existing -> {
                    if (existing == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Agreement not found"))
                                        .build());
                    }
                    if (existing.getStatus() != AgreementStatus.DRAFT
                            && existing.getStatus() != AgreementStatus.TERMINATED) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.CONFLICT)
                                        .entity(Map.of("error", "conflict",
                                                "message", "Can only delete agreements in DRAFT or TERMINATED status"))
                                        .build());
                    }
                    return agreementRepository.delete(id)
                            .map(v -> Response.noContent().build());
                });
    }

    private Agreement fromRequest(AgreementRequest request, String userId) {
        Instant now = Instant.now();
        RetryPolicy retryPolicy = null;
        if (request.retryPolicy() != null) {
            retryPolicy = new RetryPolicy(
                    request.retryPolicy().maxRetries(),
                    request.retryPolicy().backoffSeconds(),
                    request.retryPolicy().backoffMultiplier()
            );
        }
        return new Agreement(
                UUID.randomUUID(),
                request.name(),
                UUID.fromString(request.channelId()),
                request.documentType(),
                AgreementDirection.valueOf(request.direction()),
                request.packageId(),
                request.artifactId(),
                retryPolicy,
                request.slaDeadlineMinutes(),
                request.pmodeProperties(),
                AgreementStatus.DRAFT,
                0,
                now,
                userId,
                now,
                userId
        );
    }

    private void applyUpdate(Agreement existing, AgreementRequest request, String userId) {
        existing.setName(request.name());
        existing.setDocumentType(request.documentType());
        existing.setDirection(AgreementDirection.valueOf(request.direction()));
        existing.setPackageId(request.packageId());
        existing.setArtifactId(request.artifactId());
        if (request.retryPolicy() != null) {
            existing.setRetryPolicy(new RetryPolicy(
                    request.retryPolicy().maxRetries(),
                    request.retryPolicy().backoffSeconds(),
                    request.retryPolicy().backoffMultiplier()
            ));
        } else {
            existing.setRetryPolicy(null);
        }
        existing.setSlaDeadlineMinutes(request.slaDeadlineMinutes());
        existing.setPmodeProperties(request.pmodeProperties());
        existing.setModifiedAt(Instant.now());
        existing.setModifiedBy(userId);
    }

    static AgreementResponse toResponse(Agreement a) {
        AgreementResponse.RetryPolicyResponse retryPolicyResponse = null;
        if (a.getRetryPolicy() != null) {
            retryPolicyResponse = new AgreementResponse.RetryPolicyResponse(
                    a.getRetryPolicy().maxRetries(),
                    a.getRetryPolicy().backoffSeconds(),
                    a.getRetryPolicy().backoffMultiplier()
            );
        }
        return new AgreementResponse(
                a.getId(),
                a.getName(),
                a.getChannelId(),
                a.getDocumentType(),
                a.getDirection().name(),
                a.getPackageId(),
                a.getArtifactId(),
                retryPolicyResponse,
                a.getSlaDeadlineMinutes(),
                a.getPmodeProperties(),
                a.getStatus().name(),
                a.getVersion(),
                a.getCreatedAt() != null ? a.getCreatedAt().toString() : null,
                a.getCreatedBy(),
                a.getModifiedAt() != null ? a.getModifiedAt().toString() : null,
                a.getModifiedBy()
        );
    }
}
