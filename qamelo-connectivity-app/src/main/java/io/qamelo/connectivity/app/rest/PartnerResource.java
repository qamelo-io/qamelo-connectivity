package io.qamelo.connectivity.app.rest;

import io.qamelo.connectivity.app.security.JwtAuthFilter;
import io.qamelo.connectivity.app.security.auth.AuthorizationService;
import io.qamelo.connectivity.domain.partner.IdentifierScheme;
import io.qamelo.connectivity.domain.partner.IdentifierValidator;
import io.qamelo.connectivity.domain.partner.Partner;
import io.qamelo.connectivity.domain.partner.PartnerIdentifier;
import io.qamelo.connectivity.domain.partner.PartnerRepository;
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

@Path("/api/v1/partners")
@Produces(MediaType.APPLICATION_JSON)
public class PartnerResource {

    private static final Logger LOG = Logger.getLogger(PartnerResource.class);

    @Inject
    PartnerRepository partnerRepository;

    @Inject
    AuthorizationService authorizationService;

    @GET
    public Uni<List<PartnerResponse>> list(@Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "partner:read")
                .chain(() -> partnerRepository.findAll()
                        .map(PartnerResource::toResponse)
                        .collect().asList());
    }

    @GET
    @Path("/{id}")
    public Uni<Response> get(@PathParam("id") UUID id,
                             @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "partner:read")
                .chain(() -> partnerRepository.findById(id))
                .map(partner -> {
                    if (partner == null) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "not_found", "message", "Partner not found"))
                                .build();
                    }
                    return Response.ok(toResponse(partner)).build();
                });
    }

    @GET
    @Path("/by-identifier")
    public Uni<Response> findByIdentifier(@QueryParam("scheme") String scheme,
                                          @QueryParam("value") String value,
                                          @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "partner:read")
                .chain(() -> {
                    IdentifierScheme parsedScheme = IdentifierScheme.valueOf(scheme);
                    return partnerRepository.findByIdentifier(parsedScheme, value);
                })
                .map(partner -> {
                    if (partner == null) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "not_found", "message", "Partner not found for identifier"))
                                .build();
                    }
                    return Response.ok(toResponse(partner)).build();
                })
                .onFailure(IllegalArgumentException.class)
                .recoverWithItem(ex -> Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "bad_request", "message", ex.getMessage())).build());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> create(PartnerRequest request,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        LOG.infof("Creating partner: name=%s", request.name());
        return authorizationService.requirePermission(userId, "partner:manage")
                .chain(() -> {
                    Partner partner = fromRequest(request, userId);
                    return partnerRepository.save(partner);
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
    public Uni<Response> update(@PathParam("id") UUID id, PartnerRequest request,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "partner:manage")
                .chain(() -> partnerRepository.findById(id))
                .chain(existing -> {
                    if (existing == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Partner not found"))
                                        .build());
                    }
                    applyUpdate(existing, request, userId);
                    return partnerRepository.update(existing)
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
        return authorizationService.requirePermission(userId, "partner:manage")
                .chain(() -> partnerRepository.findById(id))
                .chain(existing -> {
                    if (existing == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Partner not found"))
                                        .build());
                    }
                    return partnerRepository.hasChannels(id)
                            .chain(hasChannels -> {
                                if (hasChannels) {
                                    return Uni.createFrom().item(
                                            Response.status(Response.Status.CONFLICT)
                                                    .entity(Map.of("error", "conflict",
                                                            "message", "Cannot delete partner with active channels"))
                                                    .build());
                                }
                                return partnerRepository.delete(id)
                                        .map(v -> Response.noContent().build());
                            });
                });
    }

    private Partner fromRequest(PartnerRequest request, String userId) {
        Instant now = Instant.now();
        List<PartnerIdentifier> identifiers = toIdentifiers(request.identifiers());
        // Validate each identifier
        for (PartnerIdentifier identifier : identifiers) {
            IdentifierValidator.validate(identifier);
        }
        return new Partner(
                UUID.randomUUID(),
                request.name(),
                request.description(),
                identifiers,
                now,
                userId,
                now,
                userId
        );
    }

    private void applyUpdate(Partner existing, PartnerRequest request, String userId) {
        existing.setName(request.name());
        existing.setDescription(request.description());
        List<PartnerIdentifier> identifiers = toIdentifiers(request.identifiers());
        for (PartnerIdentifier identifier : identifiers) {
            IdentifierValidator.validate(identifier);
        }
        existing.setIdentifiers(identifiers);
        existing.setModifiedAt(Instant.now());
        existing.setModifiedBy(userId);
    }

    private List<PartnerIdentifier> toIdentifiers(List<PartnerRequest.IdentifierRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream()
                .map(r -> new PartnerIdentifier(
                        UUID.randomUUID(),
                        IdentifierScheme.valueOf(r.scheme()),
                        r.customSchemeLabel(),
                        r.value()))
                .toList();
    }

    static PartnerResponse toResponse(Partner p) {
        List<PartnerResponse.IdentifierResponse> identifiers = p.getIdentifiers().stream()
                .map(id -> new PartnerResponse.IdentifierResponse(
                        id.getId(),
                        id.getScheme().name(),
                        id.getCustomSchemeLabel(),
                        id.getValue()))
                .toList();
        return new PartnerResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                identifiers,
                p.getCreatedAt() != null ? p.getCreatedAt().toString() : null,
                p.getCreatedBy(),
                p.getModifiedAt() != null ? p.getModifiedAt().toString() : null,
                p.getModifiedBy()
        );
    }
}
