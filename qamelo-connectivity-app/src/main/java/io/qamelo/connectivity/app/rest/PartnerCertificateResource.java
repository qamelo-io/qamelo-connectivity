package io.qamelo.connectivity.app.rest;

import io.qamelo.connectivity.app.security.JwtAuthFilter;
import io.qamelo.connectivity.app.security.auth.AuthorizationService;
import io.qamelo.connectivity.domain.certificate.CertificateSource;
import io.qamelo.connectivity.domain.certificate.CertificateStatus;
import io.qamelo.connectivity.domain.certificate.CertificateUsage;
import io.qamelo.connectivity.domain.certificate.ManagedCertificate;
import io.qamelo.connectivity.domain.certificate.ManagedCertificateRepository;
import io.qamelo.connectivity.domain.partner.PartnerRepository;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

@Path("/api/v1/partners/{partnerId}/certificates")
@Produces(MediaType.APPLICATION_JSON)
public class PartnerCertificateResource {

    private static final Logger LOG = Logger.getLogger(PartnerCertificateResource.class);

    @Inject
    ManagedCertificateRepository certificateRepository;

    @Inject
    PartnerRepository partnerRepository;

    @Inject
    AuthorizationService authorizationService;

    @GET
    public Uni<Response> list(@PathParam("partnerId") UUID partnerId,
                              @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "partner:read")
                .chain(() -> partnerRepository.findById(partnerId))
                .chain(partner -> {
                    if (partner == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Partner not found"))
                                        .build());
                    }
                    return certificateRepository.findByPartnerId(partnerId)
                            .map(PartnerCertificateResource::toResponse)
                            .collect().asList()
                            .map(certs -> Response.ok(certs).build());
                });
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> create(@PathParam("partnerId") UUID partnerId,
                                CertificateRequest request,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        LOG.infof("Creating certificate for partner %s: name=%s", partnerId, request.name());
        return authorizationService.requirePermission(userId, "partner:manage")
                .chain(() -> partnerRepository.findById(partnerId))
                .chain(partner -> {
                    if (partner == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Partner not found"))
                                        .build());
                    }
                    Instant now = Instant.now();
                    ManagedCertificate cert = new ManagedCertificate(
                            UUID.randomUUID(),
                            request.name(),
                            partnerId,      // partnerId
                            null,           // connectionId
                            null,           // agentId
                            CertificateUsage.valueOf(request.usage()),
                            CertificateSource.valueOf(request.source()),
                            request.vaultPath(),
                            request.serialNumber(),
                            request.subjectDn(),
                            request.issuerDn(),
                            request.notBefore() != null ? Instant.parse(request.notBefore()) : null,
                            request.notAfter() != null ? Instant.parse(request.notAfter()) : null,
                            CertificateStatus.ACTIVE,
                            0,
                            now,
                            userId,
                            now,
                            userId
                    );
                    return certificateRepository.save(cert)
                            .map(saved -> Response.status(Response.Status.CREATED)
                                    .entity(toResponse(saved)).build());
                })
                .onFailure(IllegalArgumentException.class)
                .recoverWithItem(ex -> Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "bad_request", "message", ex.getMessage())).build());
    }

    @DELETE
    @Path("/{certId}")
    public Uni<Response> delete(@PathParam("partnerId") UUID partnerId,
                                @PathParam("certId") UUID certId,
                                @Context jakarta.ws.rs.container.ContainerRequestContext ctx) {
        String userId = (String) ctx.getProperty(JwtAuthFilter.USER_ID_PROPERTY);
        return authorizationService.requirePermission(userId, "partner:manage")
                .chain(() -> certificateRepository.findById(certId))
                .chain(cert -> {
                    if (cert == null || !partnerId.equals(cert.getPartnerId())) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "not_found", "message", "Certificate not found"))
                                        .build());
                    }
                    return certificateRepository.delete(certId)
                            .map(v -> Response.noContent().build());
                });
    }

    static CertificateResponse toResponse(ManagedCertificate c) {
        return new CertificateResponse(
                c.getId(),
                c.getName(),
                c.getPartnerId(),
                c.getConnectionId(),
                c.getAgentId(),
                c.getUsage().name(),
                c.getSource().name(),
                c.getVaultPath(),
                c.getSerialNumber(),
                c.getSubjectDn(),
                c.getIssuerDn(),
                c.getNotBefore() != null ? c.getNotBefore().toString() : null,
                c.getNotAfter() != null ? c.getNotAfter().toString() : null,
                c.getStatus().name(),
                c.getVersion(),
                c.getCreatedAt() != null ? c.getCreatedAt().toString() : null,
                c.getCreatedBy(),
                c.getModifiedAt() != null ? c.getModifiedAt().toString() : null,
                c.getModifiedBy()
        );
    }
}
