package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.channel.Channel;
import io.qamelo.connectivity.domain.channel.ChannelDirection;
import io.qamelo.connectivity.domain.channel.ChannelType;

public final class ChannelMapper {

    private ChannelMapper() {}

    public static Channel toDomain(ChannelEntity entity) {
        return new Channel(
                entity.id,
                entity.name,
                entity.partnerId,
                entity.connectionId,
                ChannelType.valueOf(entity.type),
                ChannelDirection.valueOf(entity.direction),
                JsonMapUtil.fromJson(entity.properties),
                entity.enabled,
                entity.createdAt,
                entity.createdBy,
                entity.modifiedAt,
                entity.modifiedBy
        );
    }

    public static ChannelEntity toEntity(Channel channel) {
        ChannelEntity entity = new ChannelEntity();
        entity.id = channel.getId();
        entity.name = channel.getName();
        entity.partnerId = channel.getPartnerId();
        entity.connectionId = channel.getConnectionId();
        entity.type = channel.getType().name();
        entity.direction = channel.getDirection().name();
        entity.properties = JsonMapUtil.toJson(channel.getProperties());
        entity.enabled = channel.isEnabled();
        entity.createdAt = channel.getCreatedAt();
        entity.createdBy = channel.getCreatedBy();
        entity.modifiedAt = channel.getModifiedAt();
        entity.modifiedBy = channel.getModifiedBy();
        return entity;
    }
}
