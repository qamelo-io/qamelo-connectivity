package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.agent.VirtualHost;
import io.qamelo.connectivity.domain.agent.VirtualHostRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PostgresVirtualHostRepository implements VirtualHostRepository {

    private final Mutiny.SessionFactory sf;

    PostgresVirtualHostRepository(Mutiny.SessionFactory sf) {
        this.sf = sf;
    }

    @Override
    public Uni<VirtualHost> findById(UUID id) {
        return sf.withSession(s ->
                s.find(VirtualHostEntity.class, id)
                        .map(entity -> entity != null ? VirtualHostMapper.toDomain(entity) : null)
        );
    }

    @Override
    public Uni<List<VirtualHost>> findByAgentId(UUID agentId) {
        return sf.withSession(s ->
                s.createQuery("from VirtualHostEntity where agentId = :agentId order by hostname", VirtualHostEntity.class)
                        .setParameter("agentId", agentId)
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(VirtualHostMapper::toDomain)
                                .toList())
        );
    }

    @Override
    public Multi<VirtualHost> findAll() {
        return sf.withSession(s ->
                s.createQuery("from VirtualHostEntity order by hostname", VirtualHostEntity.class)
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(VirtualHostMapper::toDomain)
                                .toList())
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Override
    public Uni<VirtualHost> save(VirtualHost virtualHost) {
        VirtualHostEntity entity = VirtualHostMapper.toEntity(virtualHost);
        return sf.withTransaction(s -> s.persist(entity).replaceWith(entity))
                .map(VirtualHostMapper::toDomain);
    }

    @Override
    public Uni<VirtualHost> update(VirtualHost virtualHost) {
        VirtualHostEntity entity = VirtualHostMapper.toEntity(virtualHost);
        return sf.withTransaction(s -> s.merge(entity))
                .map(VirtualHostMapper::toDomain);
    }

    @Override
    public Uni<Void> delete(UUID id) {
        return sf.withTransaction(s ->
                s.find(VirtualHostEntity.class, id)
                        .chain(e -> e != null ? s.remove(e) : Uni.createFrom().voidItem())
        );
    }
}
