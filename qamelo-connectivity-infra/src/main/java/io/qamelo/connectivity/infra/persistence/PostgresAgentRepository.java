package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.agent.Agent;
import io.qamelo.connectivity.domain.agent.AgentRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.UUID;

@ApplicationScoped
public class PostgresAgentRepository implements AgentRepository {

    private final Mutiny.SessionFactory sf;

    PostgresAgentRepository(Mutiny.SessionFactory sf) {
        this.sf = sf;
    }

    @Override
    public Uni<Agent> findById(UUID id) {
        return sf.withSession(s ->
                s.find(AgentEntity.class, id)
                        .map(entity -> entity != null ? AgentMapper.toDomain(entity) : null)
        );
    }

    @Override
    public Multi<Agent> findAll() {
        return sf.withSession(s ->
                s.createQuery("from AgentEntity order by name", AgentEntity.class)
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(AgentMapper::toDomain)
                                .toList())
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Override
    public Uni<Agent> findByRegistrationToken(String token) {
        return sf.withSession(s ->
                s.createQuery("from AgentEntity where registrationToken = :token", AgentEntity.class)
                        .setParameter("token", token)
                        .getSingleResultOrNull()
                        .map(entity -> entity != null ? AgentMapper.toDomain(entity) : null)
        );
    }

    @Override
    public Uni<Agent> save(Agent agent) {
        AgentEntity entity = AgentMapper.toEntity(agent);
        return sf.withTransaction(s -> s.persist(entity).replaceWith(entity))
                .map(AgentMapper::toDomain);
    }

    @Override
    public Uni<Agent> update(Agent agent) {
        AgentEntity entity = AgentMapper.toEntity(agent);
        return sf.withTransaction(s -> s.merge(entity))
                .map(AgentMapper::toDomain);
    }

    @Override
    public Uni<Void> delete(UUID id) {
        return sf.withTransaction(s ->
                s.find(AgentEntity.class, id)
                        .chain(e -> e != null ? s.remove(e) : Uni.createFrom().voidItem())
        );
    }

    @Override
    public Uni<Boolean> hasVirtualHosts(UUID agentId) {
        return sf.withSession(s ->
                s.createQuery("select count(v) from VirtualHostEntity v where v.agentId = :agentId", Long.class)
                        .setParameter("agentId", agentId)
                        .getSingleResult()
                        .map(count -> count > 0)
        );
    }
}
