package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.agreement.Agreement;
import io.qamelo.connectivity.domain.agreement.AgreementRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.UUID;

@ApplicationScoped
public class PostgresAgreementRepository implements AgreementRepository {

    private final Mutiny.SessionFactory sf;

    PostgresAgreementRepository(Mutiny.SessionFactory sf) {
        this.sf = sf;
    }

    @Override
    public Uni<Agreement> findById(UUID id) {
        return sf.withSession(s ->
                s.find(AgreementEntity.class, id)
                        .map(entity -> entity != null ? AgreementMapper.toDomain(entity) : null)
        );
    }

    @Override
    public Multi<Agreement> findAll() {
        return sf.withSession(s ->
                s.createQuery("from AgreementEntity order by name", AgreementEntity.class)
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(AgreementMapper::toDomain)
                                .toList())
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Override
    public Multi<Agreement> findByChannelId(UUID channelId) {
        return sf.withSession(s ->
                s.createQuery("from AgreementEntity where channelId = :channelId order by name", AgreementEntity.class)
                        .setParameter("channelId", channelId)
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(AgreementMapper::toDomain)
                                .toList())
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Override
    public Uni<Agreement> save(Agreement agreement) {
        AgreementEntity entity = AgreementMapper.toEntity(agreement);
        return sf.withTransaction(s -> s.persist(entity).replaceWith(entity))
                .map(AgreementMapper::toDomain);
    }

    @Override
    public Uni<Agreement> update(Agreement agreement) {
        AgreementEntity entity = AgreementMapper.toEntity(agreement);
        return sf.withTransaction(s -> s.merge(entity))
                .map(AgreementMapper::toDomain);
    }

    @Override
    public Uni<Void> delete(UUID id) {
        return sf.withTransaction(s ->
                s.find(AgreementEntity.class, id)
                        .chain(e -> e != null ? s.remove(e) : Uni.createFrom().voidItem())
        );
    }
}
