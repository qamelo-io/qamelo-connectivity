package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.test.ConnectivityTest;
import io.qamelo.connectivity.domain.test.ConnectivityTestRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.UUID;

@ApplicationScoped
public class PostgresConnectivityTestRepository implements ConnectivityTestRepository {

    private final Mutiny.SessionFactory sf;

    PostgresConnectivityTestRepository(Mutiny.SessionFactory sf) {
        this.sf = sf;
    }

    @Override
    public Uni<ConnectivityTest> save(ConnectivityTest test) {
        ConnectivityTestEntity entity = ConnectivityTestMapper.toEntity(test);
        return sf.withTransaction(s -> s.persist(entity).replaceWith(entity))
                .map(ConnectivityTestMapper::toDomain);
    }

    @Override
    public Uni<ConnectivityTest> update(ConnectivityTest test) {
        ConnectivityTestEntity entity = ConnectivityTestMapper.toEntity(test);
        return sf.withTransaction(s -> s.merge(entity))
                .map(ConnectivityTestMapper::toDomain);
    }

    @Override
    public Multi<ConnectivityTest> findByConnectionId(UUID connectionId) {
        return sf.withSession(s ->
                s.createQuery("from ConnectivityTestEntity where connectionId = :connId order by startedAt desc",
                                ConnectivityTestEntity.class)
                        .setParameter("connId", connectionId)
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(ConnectivityTestMapper::toDomain)
                                .toList())
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }
}
