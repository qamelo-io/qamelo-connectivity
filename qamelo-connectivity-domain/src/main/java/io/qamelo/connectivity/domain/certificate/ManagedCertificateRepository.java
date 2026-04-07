package io.qamelo.connectivity.domain.certificate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.UUID;

public interface ManagedCertificateRepository {

    Uni<ManagedCertificate> findById(UUID id);

    Multi<ManagedCertificate> findByPartnerId(UUID partnerId);

    Multi<ManagedCertificate> findByConnectionId(UUID connectionId);

    Multi<ManagedCertificate> findByAgentId(UUID agentId);

    Multi<ManagedCertificate> findByStatus(CertificateStatus status);

    Multi<ManagedCertificate> findAll();

    Uni<ManagedCertificate> save(ManagedCertificate cert);

    Uni<ManagedCertificate> update(ManagedCertificate cert);

    Uni<Void> delete(UUID id);

    Multi<ManagedCertificate> findExpiringSoon(Instant threshold);

    Multi<ManagedCertificate> findExpired(Instant now);
}
