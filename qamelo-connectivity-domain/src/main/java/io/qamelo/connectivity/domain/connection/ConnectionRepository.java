package io.qamelo.connectivity.domain.connection;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface ConnectionRepository {

    Uni<Connection> findById(UUID id);

    Multi<Connection> findAll();

    Uni<Connection> save(Connection connection);

    Uni<Connection> update(Connection connection);

    Uni<Void> delete(UUID id);
}
