package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.partner.IdentifierScheme;
import io.qamelo.connectivity.domain.partner.Partner;
import io.qamelo.connectivity.domain.partner.PartnerRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PostgresPartnerRepository implements PartnerRepository {

    private final Mutiny.SessionFactory sf;

    PostgresPartnerRepository(Mutiny.SessionFactory sf) {
        this.sf = sf;
    }

    @Override
    public Uni<Partner> findById(UUID id) {
        return sf.withSession(s ->
                s.find(PartnerEntity.class, id)
                        .chain(entity -> {
                            if (entity == null) return Uni.createFrom().nullItem();
                            return loadIdentifiers(s, id)
                                    .map(ids -> PartnerMapper.toDomain(entity, ids));
                        })
        );
    }

    @Override
    public Multi<Partner> findAll() {
        return sf.withSession(s ->
                s.createQuery("from PartnerEntity order by name", PartnerEntity.class)
                        .getResultList()
                        .chain(entities -> {
                            if (entities.isEmpty()) {
                                return Uni.createFrom().item(List.<Partner>of());
                            }
                            List<Uni<Partner>> unis = entities.stream()
                                    .map(entity -> loadIdentifiers(s, entity.id)
                                            .map(ids -> PartnerMapper.toDomain(entity, ids)))
                                    .toList();
                            return Uni.join().all(unis).andFailFast();
                        })
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Override
    public Uni<Partner> findByIdentifier(IdentifierScheme scheme, String value) {
        return sf.withSession(s ->
                s.createQuery(
                                "select pi.partnerId from PartnerIdentifierEntity pi " +
                                        "where pi.scheme = :scheme and pi.value = :value",
                                UUID.class)
                        .setParameter("scheme", scheme.name())
                        .setParameter("value", value)
                        .getSingleResultOrNull()
                        .chain(partnerId -> {
                            if (partnerId == null) return Uni.createFrom().nullItem();
                            return s.find(PartnerEntity.class, partnerId)
                                    .chain(entity -> {
                                        if (entity == null) return Uni.createFrom().nullItem();
                                        return loadIdentifiers(s, partnerId)
                                                .map(ids -> PartnerMapper.toDomain(entity, ids));
                                    });
                        })
        );
    }

    @Override
    public Uni<Partner> save(Partner partner) {
        PartnerEntity entity = PartnerMapper.toEntity(partner);
        List<PartnerIdentifierEntity> identifierEntities = PartnerMapper.toIdentifierEntities(partner);
        return sf.withTransaction(s ->
                s.persist(entity)
                        .chain(() -> persistIdentifiers(s, identifierEntities))
                        .chain(() -> loadIdentifiers(s, entity.id))
                        .map(ids -> PartnerMapper.toDomain(entity, ids))
        );
    }

    @Override
    public Uni<Partner> update(Partner partner) {
        PartnerEntity entity = PartnerMapper.toEntity(partner);
        List<PartnerIdentifierEntity> identifierEntities = PartnerMapper.toIdentifierEntities(partner);
        return sf.withTransaction(s ->
                s.merge(entity)
                        .chain(merged -> deleteIdentifiers(s, partner.getId()))
                        .chain(() -> persistIdentifiers(s, identifierEntities))
                        .chain(() -> loadIdentifiers(s, entity.id))
                        .map(ids -> PartnerMapper.toDomain(entity, ids))
        );
    }

    @Override
    public Uni<Void> delete(UUID id) {
        return sf.withTransaction(s ->
                deleteIdentifiers(s, id)
                        .chain(() -> s.find(PartnerEntity.class, id))
                        .chain(e -> e != null ? s.remove(e) : Uni.createFrom().voidItem())
        );
    }

    @Override
    public Uni<Boolean> hasChannels(UUID partnerId) {
        return sf.withSession(s ->
                s.createQuery("select count(c) from ChannelEntity c where c.partnerId = :partnerId", Long.class)
                        .setParameter("partnerId", partnerId)
                        .getSingleResult()
                        .map(count -> count > 0)
        );
    }

    private Uni<List<PartnerIdentifierEntity>> loadIdentifiers(Mutiny.Session s, UUID partnerId) {
        return s.createQuery(
                        "from PartnerIdentifierEntity where partnerId = :partnerId",
                        PartnerIdentifierEntity.class)
                .setParameter("partnerId", partnerId)
                .getResultList();
    }

    private Uni<Void> deleteIdentifiers(Mutiny.Session s, UUID partnerId) {
        return s.createQuery("delete from PartnerIdentifierEntity where partnerId = :partnerId")
                .setParameter("partnerId", partnerId)
                .executeUpdate()
                .replaceWithVoid();
    }

    private Uni<Void> persistIdentifiers(Mutiny.Session s, List<PartnerIdentifierEntity> identifiers) {
        if (identifiers.isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        Uni<Void> chain = Uni.createFrom().voidItem();
        for (PartnerIdentifierEntity ide : identifiers) {
            chain = chain.chain(() -> s.persist(ide));
        }
        return chain;
    }
}
