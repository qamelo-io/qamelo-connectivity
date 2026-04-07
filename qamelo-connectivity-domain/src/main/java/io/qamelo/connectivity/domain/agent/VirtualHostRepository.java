package io.qamelo.connectivity.domain.agent;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

public interface VirtualHostRepository {

    Uni<VirtualHost> findById(UUID id);

    Uni<List<VirtualHost>> findByAgentId(UUID agentId);

    Multi<VirtualHost> findAll();

    Uni<VirtualHost> save(VirtualHost virtualHost);

    Uni<VirtualHost> update(VirtualHost virtualHost);

    Uni<Void> delete(UUID id);
}
