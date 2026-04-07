package io.qamelo.connectivity.app.rest.internal;

import io.qamelo.connectivity.domain.agreement.Agreement;
import io.qamelo.connectivity.domain.agreement.AgreementRepository;
import io.qamelo.connectivity.domain.channel.Channel;
import io.qamelo.connectivity.domain.channel.ChannelRepository;
import io.qamelo.connectivity.domain.connection.Connection;
import io.qamelo.connectivity.domain.connection.ConnectionRepository;
import io.qamelo.connectivity.domain.partner.Partner;
import io.qamelo.connectivity.domain.partner.PartnerIdentifier;
import io.qamelo.connectivity.domain.partner.PartnerRepository;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/internal/agreements")
@Produces(MediaType.APPLICATION_JSON)
public class InternalAgreementResource {

    @Inject
    AgreementRepository agreementRepository;

    @Inject
    ChannelRepository channelRepository;

    @Inject
    ConnectionRepository connectionRepository;

    @Inject
    PartnerRepository partnerRepository;

    @GET
    @Path("/{id}")
    public Uni<Response> getById(@PathParam("id") UUID id) {
        return agreementRepository.findById(id)
                .chain(agreement -> {
                    if (agreement == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found",
                                                "message", "Agreement not found"))
                                        .build());
                    }
                    return buildDenormalizedResponse(agreement)
                            .map(resp -> Response.ok(resp).build());
                });
    }

    @POST
    @Path("/match")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> match(AgreementMatchRequest request) {
        // Step 1: Find all agreements, then filter by channel type and criteria match
        return agreementRepository.findAll()
                .collect().asList()
                .chain(allAgreements -> {
                    // Step 2: For each agreement, load its channel to check the type
                    List<Uni<AgreementWithChannel>> lookups = allAgreements.stream()
                            .map(agreement -> channelRepository.findById(agreement.getChannelId())
                                    .map(channel -> new AgreementWithChannel(agreement, channel)))
                            .toList();
                    if (lookups.isEmpty()) {
                        return Uni.createFrom().item(List.<AgreementWithChannel>of());
                    }
                    return Uni.join().all(lookups).andFailFast();
                })
                .chain(pairs -> {
                    // Step 3: Filter by channel type and pmode criteria
                    List<Agreement> matches = pairs.stream()
                            .filter(pair -> pair.channel != null)
                            .filter(pair -> pair.channel.getType().name().equals(request.channelType()))
                            .filter(pair -> matchesCriteria(pair.agreement, request.criteria()))
                            .map(pair -> pair.agreement)
                            .toList();

                    if (matches.isEmpty()) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found",
                                                "message", "No agreement matches the given criteria"))
                                        .build());
                    }
                    if (matches.size() > 1) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.CONFLICT)
                                        .entity(Map.of("error", "conflict",
                                                "message", "Ambiguous match: " + matches.size()
                                                        + " agreements match the given criteria"))
                                        .build());
                    }
                    return buildDenormalizedResponse(matches.get(0))
                            .map(resp -> Response.ok(resp).build());
                });
    }

    private boolean matchesCriteria(Agreement agreement, Map<String, String> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return true;
        }
        Map<String, String> pmodeProps = agreement.getPmodeProperties();
        for (Map.Entry<String, String> entry : criteria.entrySet()) {
            String pmodeValue = pmodeProps.get(entry.getKey());
            if (pmodeValue == null || !pmodeValue.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private Uni<AgreementResolutionResponse> buildDenormalizedResponse(Agreement agreement) {
        return channelRepository.findById(agreement.getChannelId())
                .chain(channel -> {
                    Uni<Connection> connectionUni = connectionRepository.findById(channel.getConnectionId());
                    Uni<Partner> partnerUni = partnerRepository.findById(channel.getPartnerId());
                    return Uni.combine().all().unis(connectionUni, partnerUni)
                            .asTuple()
                            .map(tuple -> toDenormalized(agreement, channel, tuple.getItem1(), tuple.getItem2()));
                });
    }

    private AgreementResolutionResponse toDenormalized(Agreement a, Channel ch, Connection conn, Partner p) {
        AgreementResolutionResponse.RetryPolicyResponse retryPolicy = null;
        if (a.getRetryPolicy() != null) {
            retryPolicy = new AgreementResolutionResponse.RetryPolicyResponse(
                    a.getRetryPolicy().maxRetries(),
                    a.getRetryPolicy().backoffSeconds(),
                    a.getRetryPolicy().backoffMultiplier()
            );
        }
        List<AgreementResolutionResponse.IdentifierEntry> identifiers = List.of();
        if (p != null && p.getIdentifiers() != null) {
            identifiers = p.getIdentifiers().stream()
                    .map(id -> new AgreementResolutionResponse.IdentifierEntry(
                            id.getScheme().name(),
                            id.getCustomSchemeLabel(),
                            id.getValue()))
                    .toList();
        }
        return new AgreementResolutionResponse(
                a.getId(),
                a.getName(),
                a.getDocumentType(),
                a.getDirection().name(),
                a.getPackageId(),
                a.getArtifactId(),
                retryPolicy,
                a.getSlaDeadlineMinutes(),
                a.getPmodeProperties(),
                a.getStatus().name(),
                ch.getId(),
                ch.getName(),
                ch.getType().name(),
                ch.getDirection().name(),
                ch.getProperties(),
                ch.isEnabled(),
                conn.getId(),
                conn.getName(),
                conn.getType().name(),
                conn.getHost(),
                conn.getPort(),
                conn.getAuthType().name(),
                conn.getVaultCredentialPath(),
                p != null ? p.getId() : null,
                p != null ? p.getName() : null,
                identifiers
        );
    }

    private record AgreementWithChannel(Agreement agreement, Channel channel) {}
}
