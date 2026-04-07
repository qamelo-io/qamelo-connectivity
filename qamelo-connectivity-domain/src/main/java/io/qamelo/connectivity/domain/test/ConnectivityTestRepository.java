package io.qamelo.connectivity.domain.test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface ConnectivityTestRepository {

    Uni<ConnectivityTest> save(ConnectivityTest test);

    Uni<ConnectivityTest> update(ConnectivityTest test);

    Multi<ConnectivityTest> findByConnectionId(UUID connectionId);
}
