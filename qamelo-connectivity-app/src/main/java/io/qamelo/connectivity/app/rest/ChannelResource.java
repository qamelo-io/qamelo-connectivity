package io.qamelo.connectivity.app.rest;

import io.qamelo.connectivity.app.security.JwtAuthFilter;
import io.qamelo.connectivity.app.security.auth.AuthorizationService;
import io.qamelo.connectivity.domain.channel.Channel;
import io.qamelo.connectivity.domain.channel.ChannelDirection;
import io.qamelo.connectivity.domain.channel.ChannelRepository;
import io.qamelo.connectivity.domain.channel.ChannelType;
import io.qamelo.connectivity.domain.connection.ConnectionRepository;
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

@Path("/api/v1/channels")
@Produces(MediaType.APPLICATION_JSON)
public class ChannelResource {

    private static final Logger LOG = Logger.getLogger(ChannelResource.class);

    @Inject
    ChannelRepository channelRepository;

    @Inject
    PartnerRepository partnerRepository;

    @Inject
    ConnectionRepository connectionRepository;

    @Inject
    AuthorizationService authorizationService;

    @GET
    public Uni<List<ChannelResponse>> list(@QueryParam("partnerId") String partnerId,
                                           @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "channel:read")
                .chain(() -> {
                    if (partnerId != null && !partnerId.isBlank()) {
                        return channelRepository.findByPartnerId(UUID.fromString(partnerId))
                                .map(ChannelResource::toResponse)
                                .collect().asList();
                    }
                    return channelRepository.findAll()
                            .map(ChannelResource::toResponse)
                            .collect().asList();
                });
    }

    @GET
    @Path("/{id}")
    public Uni<Response> get(@PathParam("id") UUID id,
                             @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "channel:read")
                .chain(() -> channelRepository.findById(id))
                .map(channel -> {
                    if (channel == null) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "not_found", "message", "Channel not found"))
                                .build();
                    }
                    return Response.ok(toResponse(channel)).build();
                });
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> create(ChannelRequest request,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        LOG.infof("Creating channel: name=%s, type=%s", request.name(), request.type());
        return authorizationService.requirePermission(userId, "channel:manage")
                .chain(() -> {
                    UUID partnerId = UUID.fromString(request.partnerId());
                    UUID connectionId = UUID.fromString(request.connectionId());
                    return partnerRepository.findById(partnerId)
                            .chain(partner -> {
                                if (partner == null) {
                                    return Uni.createFrom().item(
                                            Response.status(Response.Status.BAD_REQUEST)
                                                    .entity(Map.of("error", "bad_request",
                                                            "message", "Partner not found: " + partnerId))
                                                    .build());
                                }
                                return connectionRepository.findById(connectionId)
                                        .chain(connection -> {
                                            if (connection == null) {
                                                return Uni.createFrom().item(
                                                        Response.status(Response.Status.BAD_REQUEST)
                                                                .entity(Map.of("error", "bad_request",
                                                                        "message", "Connection not found: " + connectionId))
                                                                .build());
                                            }
                                            Channel channel = fromRequest(request, userId);
                                            return channelRepository.save(channel)
                                                    .map(saved -> Response.status(Response.Status.CREATED)
                                                            .entity(toResponse(saved)).build());
                                        });
                            });
                })
                .onFailure(IllegalArgumentException.class)
                .recoverWithItem(ex -> Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "bad_request", "message", ex.getMessage())).build());
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> update(@PathParam("id") UUID id, ChannelRequest request,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "channel:manage")
                .chain(() -> channelRepository.findById(id))
                .chain(existing -> {
                    if (existing == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Channel not found"))
                                        .build());
                    }
                    UUID partnerId = UUID.fromString(request.partnerId());
                    UUID connectionId = UUID.fromString(request.connectionId());
                    return partnerRepository.findById(partnerId)
                            .chain(partner -> {
                                if (partner == null) {
                                    return Uni.createFrom().item(
                                            Response.status(Response.Status.BAD_REQUEST)
                                                    .entity(Map.of("error", "bad_request",
                                                            "message", "Partner not found: " + partnerId))
                                                    .build());
                                }
                                return connectionRepository.findById(connectionId)
                                        .chain(connection -> {
                                            if (connection == null) {
                                                return Uni.createFrom().item(
                                                        Response.status(Response.Status.BAD_REQUEST)
                                                                .entity(Map.of("error", "bad_request",
                                                                        "message", "Connection not found: " + connectionId))
                                                                .build());
                                            }
                                            applyUpdate(existing, request, userId);
                                            return channelRepository.update(existing)
                                                    .map(updated -> Response.ok(toResponse(updated)).build());
                                        });
                            });
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
        return authorizationService.requirePermission(userId, "channel:manage")
                .chain(() -> channelRepository.findById(id))
                .chain(existing -> {
                    if (existing == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Channel not found"))
                                        .build());
                    }
                    return channelRepository.hasAgreements(id)
                            .chain(hasAgreements -> {
                                if (hasAgreements) {
                                    return Uni.createFrom().item(
                                            Response.status(Response.Status.CONFLICT)
                                                    .entity(Map.of("error", "conflict",
                                                            "message", "Cannot delete channel with active agreements"))
                                                    .build());
                                }
                                return channelRepository.delete(id)
                                        .map(v -> Response.noContent().build());
                            });
                });
    }

    private Channel fromRequest(ChannelRequest request, String userId) {
        Instant now = Instant.now();
        return new Channel(
                UUID.randomUUID(),
                request.name(),
                UUID.fromString(request.partnerId()),
                UUID.fromString(request.connectionId()),
                ChannelType.valueOf(request.type()),
                ChannelDirection.valueOf(request.direction()),
                request.properties(),
                request.enabled() != null ? request.enabled() : true,
                now,
                userId,
                now,
                userId
        );
    }

    private void applyUpdate(Channel existing, ChannelRequest request, String userId) {
        existing.setName(request.name());
        existing.setPartnerId(UUID.fromString(request.partnerId()));
        existing.setConnectionId(UUID.fromString(request.connectionId()));
        existing.setType(ChannelType.valueOf(request.type()));
        existing.setDirection(ChannelDirection.valueOf(request.direction()));
        existing.setProperties(request.properties());
        existing.setEnabled(request.enabled() != null ? request.enabled() : existing.isEnabled());
        existing.setModifiedAt(Instant.now());
        existing.setModifiedBy(userId);
    }

    static ChannelResponse toResponse(Channel c) {
        return new ChannelResponse(
                c.getId(),
                c.getName(),
                c.getPartnerId(),
                c.getConnectionId(),
                c.getType().name(),
                c.getDirection().name(),
                c.getProperties(),
                c.isEnabled(),
                c.getCreatedAt() != null ? c.getCreatedAt().toString() : null,
                c.getCreatedBy(),
                c.getModifiedAt() != null ? c.getModifiedAt().toString() : null,
                c.getModifiedBy()
        );
    }
}
