package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.certificate.CertificateStatus;
import io.qamelo.connectivity.domain.certificate.ManagedCertificate;
import io.qamelo.connectivity.domain.certificate.ManagedCertificateRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class PostgresManagedCertificateRepository implements ManagedCertificateRepository {

    private final Mutiny.SessionFactory sf;

    PostgresManagedCertificateRepository(Mutiny.SessionFactory sf) {
        this.sf = sf;
    }

    @Override
    public Uni<ManagedCertificate> findById(UUID id) {
        return sf.withSession(s ->
                s.find(ManagedCertificateEntity.class, id)
                        .map(entity -> entity != null ? ManagedCertificateMapper.toDomain(entity) : null)
        );
    }

    @Override
    public Multi<ManagedCertificate> findByPartnerId(UUID partnerId) {
        return sf.withSession(s ->
                s.createQuery("from ManagedCertificateEntity where partnerId = :partnerId order by name",
                                ManagedCertificateEntity.class)
                        .setParameter("partnerId", partnerId)
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(ManagedCertificateMapper::toDomain)
                                .toList())
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Override
    public Multi<ManagedCertificate> findByConnectionId(UUID connectionId) {
        return sf.withSession(s ->
                s.createQuery("from ManagedCertificateEntity where connectionId = :connectionId order by name",
                                ManagedCertificateEntity.class)
                        .setParameter("connectionId", connectionId)
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(ManagedCertificateMapper::toDomain)
                                .toList())
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Override
    public Multi<ManagedCertificate> findByAgentId(UUID agentId) {
        return sf.withSession(s ->
                s.createQuery("from ManagedCertificateEntity where agentId = :agentId order by name",
                                ManagedCertificateEntity.class)
                        .setParameter("agentId", agentId)
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(ManagedCertificateMapper::toDomain)
                                .toList())
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Override
    public Multi<ManagedCertificate> findByStatus(CertificateStatus status) {
        return sf.withSession(s ->
                s.createQuery("from ManagedCertificateEntity where status = :status order by name",
                                ManagedCertificateEntity.class)
                        .setParameter("status", status.name())
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(ManagedCertificateMapper::toDomain)
                                .toList())
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Override
    public Multi<ManagedCertificate> findAll() {
        return sf.withSession(s ->
                s.createQuery("from ManagedCertificateEntity order by name", ManagedCertificateEntity.class)
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(ManagedCertificateMapper::toDomain)
                                .toList())
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Override
    public Uni<ManagedCertificate> save(ManagedCertificate cert) {
        ManagedCertificateEntity entity = ManagedCertificateMapper.toEntity(cert);
        return sf.withTransaction(s -> s.persist(entity).replaceWith(entity))
                .map(ManagedCertificateMapper::toDomain);
    }

    @Override
    public Uni<ManagedCertificate> update(ManagedCertificate cert) {
        ManagedCertificateEntity entity = ManagedCertificateMapper.toEntity(cert);
        return sf.withTransaction(s -> s.merge(entity))
                .map(ManagedCertificateMapper::toDomain);
    }

    @Override
    public Uni<Void> delete(UUID id) {
        return sf.withTransaction(s ->
                s.find(ManagedCertificateEntity.class, id)
                        .chain(e -> e != null ? s.remove(e) : Uni.createFrom().voidItem())
        );
    }

    @Override
    public Multi<ManagedCertificate> findExpiringSoon(Instant threshold) {
        return sf.withSession(s ->
                s.createQuery("from ManagedCertificateEntity where notAfter < :threshold and status = :status",
                                ManagedCertificateEntity.class)
                        .setParameter("threshold", threshold)
                        .setParameter("status", CertificateStatus.ACTIVE.name())
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(ManagedCertificateMapper::toDomain)
                                .toList())
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Override
    public Multi<ManagedCertificate> findExpired(Instant now) {
        return sf.withSession(s ->
                s.createQuery("from ManagedCertificateEntity where notAfter < :now and status in (:statuses)",
                                ManagedCertificateEntity.class)
                        .setParameter("now", now)
                        .setParameter("statuses", java.util.List.of(
                                CertificateStatus.ACTIVE.name(),
                                CertificateStatus.EXPIRING_SOON.name()))
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(ManagedCertificateMapper::toDomain)
                                .toList())
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }
}
