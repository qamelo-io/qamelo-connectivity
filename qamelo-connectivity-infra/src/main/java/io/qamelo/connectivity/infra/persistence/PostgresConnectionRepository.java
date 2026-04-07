package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.connection.Connection;
import io.qamelo.connectivity.domain.connection.ConnectionRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.UUID;

@ApplicationScoped
public class PostgresConnectionRepository implements ConnectionRepository {

    private final Mutiny.SessionFactory sf;

    PostgresConnectionRepository(Mutiny.SessionFactory sf) {
        this.sf = sf;
    }

    @Override
    public Uni<Connection> findById(UUID id) {
        return sf.withSession(s ->
                s.find(ConnectionEntity.class, id)
                        .map(entity -> entity != null ? ConnectionMapper.toDomain(entity) : null)
        );
    }

    @Override
    public Multi<Connection> findAll() {
        return sf.withSession(s ->
                s.createQuery("from ConnectionEntity order by name", ConnectionEntity.class)
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(ConnectionMapper::toDomain)
                                .toList())
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Override
    public Uni<Connection> save(Connection connection) {
        ConnectionEntity entity = ConnectionMapper.toEntity(connection);
        return sf.withTransaction(s -> s.persist(entity).replaceWith(entity))
                .map(ConnectionMapper::toDomain);
    }

    @Override
    public Uni<Connection> update(Connection connection) {
        ConnectionEntity entity = ConnectionMapper.toEntity(connection);
        return sf.withTransaction(s -> s.merge(entity))
                .map(ConnectionMapper::toDomain);
    }

    @Override
    public Uni<Void> delete(UUID id) {
        return sf.withTransaction(s ->
                s.find(ConnectionEntity.class, id)
                        .chain(e -> e != null ? s.remove(e) : Uni.createFrom().voidItem())
        );
    }
}
