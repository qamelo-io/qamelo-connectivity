package io.qamelo.connectivity.app.reconciler;

import io.qamelo.connectivity.domain.certificate.CertificateStatus;
import io.qamelo.connectivity.domain.certificate.ManagedCertificateRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;

@ApplicationScoped
public class CertificateExpirationScheduler {

    private static final Logger LOG = Logger.getLogger(CertificateExpirationScheduler.class);
    private static final Duration EXPIRING_SOON_THRESHOLD = Duration.ofDays(30);

    @Inject
    ManagedCertificateRepository certificateRepository;

    @io.quarkus.scheduler.Scheduled(every = "1h")
    public Uni<Void> checkExpirations() {
        Instant now = Instant.now();
        Instant threshold = now.plus(EXPIRING_SOON_THRESHOLD);

        // Phase 1: ACTIVE -> EXPIRING_SOON (notAfter < now + 30 days)
        Uni<Void> expiringSoon = certificateRepository.findExpiringSoon(threshold)
                .onItem().transformToUniAndConcatenate(cert -> {
                    LOG.infof("{\"event\":\"cert_expiring_soon\",\"certId\":\"%s\",\"name\":\"%s\",\"notAfter\":\"%s\"}",
                            cert.getId(), cert.getName(), cert.getNotAfter());
                    cert.setStatus(CertificateStatus.EXPIRING_SOON);
                    cert.setModifiedAt(Instant.now());
                    return certificateRepository.update(cert).replaceWithVoid();
                })
                .collect().last()
                .replaceWithVoid();

        // Phase 2: ACTIVE/EXPIRING_SOON -> EXPIRED (notAfter < now)
        Uni<Void> expired = certificateRepository.findExpired(now)
                .onItem().transformToUniAndConcatenate(cert -> {
                    LOG.infof("{\"event\":\"cert_expired\",\"certId\":\"%s\",\"name\":\"%s\",\"notAfter\":\"%s\"}",
                            cert.getId(), cert.getName(), cert.getNotAfter());
                    cert.setStatus(CertificateStatus.EXPIRED);
                    cert.setModifiedAt(Instant.now());
                    return certificateRepository.update(cert).replaceWithVoid();
                })
                .collect().last()
                .replaceWithVoid();

        return expiringSoon.chain(() -> expired);
    }
}
