package io.qamelo.connectivity.domain.agent;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface AgentRepository {

    Uni<Agent> findById(UUID id);

    Multi<Agent> findAll();

    Uni<Agent> findByRegistrationToken(String token);

    Uni<Agent> save(Agent agent);

    Uni<Agent> update(Agent agent);

    Uni<Void> delete(UUID id);

    // TODO Phase 6: query virtual_hosts table when it exists
    Uni<Boolean> hasVirtualHosts(UUID agentId);
}
