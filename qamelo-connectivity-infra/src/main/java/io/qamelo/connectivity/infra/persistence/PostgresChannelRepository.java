package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.channel.Channel;
import io.qamelo.connectivity.domain.channel.ChannelRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.UUID;

@ApplicationScoped
public class PostgresChannelRepository implements ChannelRepository {

    private final Mutiny.SessionFactory sf;

    PostgresChannelRepository(Mutiny.SessionFactory sf) {
        this.sf = sf;
    }

    @Override
    public Uni<Channel> findById(UUID id) {
        return sf.withSession(s ->
                s.find(ChannelEntity.class, id)
                        .map(entity -> entity != null ? ChannelMapper.toDomain(entity) : null)
        );
    }

    @Override
    public Multi<Channel> findAll() {
        return sf.withSession(s ->
                s.createQuery("from ChannelEntity order by name", ChannelEntity.class)
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(ChannelMapper::toDomain)
                                .toList())
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Override
    public Multi<Channel> findByPartnerId(UUID partnerId) {
        return sf.withSession(s ->
                s.createQuery("from ChannelEntity where partnerId = :partnerId order by name", ChannelEntity.class)
                        .setParameter("partnerId", partnerId)
                        .getResultList()
                        .map(entities -> entities.stream()
                                .map(ChannelMapper::toDomain)
                                .toList())
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Override
    public Uni<Channel> save(Channel channel) {
        ChannelEntity entity = ChannelMapper.toEntity(channel);
        return sf.withTransaction(s -> s.persist(entity).replaceWith(entity))
                .map(ChannelMapper::toDomain);
    }

    @Override
    public Uni<Channel> update(Channel channel) {
        ChannelEntity entity = ChannelMapper.toEntity(channel);
        return sf.withTransaction(s -> s.merge(entity))
                .map(ChannelMapper::toDomain);
    }

    @Override
    public Uni<Void> delete(UUID id) {
        return sf.withTransaction(s ->
                s.find(ChannelEntity.class, id)
                        .chain(e -> e != null ? s.remove(e) : Uni.createFrom().voidItem())
        );
    }

    @Override
    public Uni<Boolean> hasAgreements(UUID channelId) {
        return sf.withSession(s ->
                s.createQuery("select count(a) from AgreementEntity a where a.channelId = :channelId", Long.class)
                        .setParameter("channelId", channelId)
                        .getSingleResult()
                        .map(count -> count > 0)
        );
    }
}
