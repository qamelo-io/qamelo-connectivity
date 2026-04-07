package io.qamelo.connectivity.domain.channel;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface ChannelRepository {

    Uni<Channel> findById(UUID id);

    Multi<Channel> findAll();

    Multi<Channel> findByPartnerId(UUID partnerId);

    Uni<Channel> save(Channel channel);

    Uni<Channel> update(Channel channel);

    Uni<Void> delete(UUID id);

    Uni<Boolean> hasAgreements(UUID channelId);
}
