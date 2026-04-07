package io.qamelo.connectivity.domain.channel;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class Channel {

    private final UUID id;
    private String name;
    private UUID partnerId;
    private UUID connectionId;
    private ChannelType type;
    private ChannelDirection direction;
    private Map<String, String> properties;
    private boolean enabled;
    private final Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;

    public Channel(UUID id, String name, UUID partnerId, UUID connectionId,
                   ChannelType type, ChannelDirection direction,
                   Map<String, String> properties, boolean enabled,
                   Instant createdAt, String createdBy, Instant modifiedAt, String modifiedBy) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        setName(name);
        this.partnerId = Objects.requireNonNull(partnerId, "PartnerId must not be null");
        this.connectionId = Objects.requireNonNull(connectionId, "ConnectionId must not be null");
        this.type = Objects.requireNonNull(type, "Type must not be null");
        this.direction = Objects.requireNonNull(direction, "Direction must not be null");
        this.properties = properties != null ? Map.copyOf(properties) : Map.of();
        this.enabled = enabled;
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt must not be null");
        this.createdBy = createdBy;
        this.modifiedAt = modifiedAt;
        this.modifiedBy = modifiedBy;
    }

    public UUID getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) {
        Objects.requireNonNull(name, "Name must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("Name must not be blank");
        this.name = name;
    }

    public UUID getPartnerId() { return partnerId; }
    public void setPartnerId(UUID partnerId) { this.partnerId = Objects.requireNonNull(partnerId); }

    public UUID getConnectionId() { return connectionId; }
    public void setConnectionId(UUID connectionId) { this.connectionId = Objects.requireNonNull(connectionId); }

    public ChannelType getType() { return type; }
    public void setType(ChannelType type) { this.type = Objects.requireNonNull(type); }

    public ChannelDirection getDirection() { return direction; }
    public void setDirection(ChannelDirection direction) { this.direction = Objects.requireNonNull(direction); }

    public Map<String, String> getProperties() { return properties; }
    public void setProperties(Map<String, String> properties) {
        this.properties = properties != null ? Map.copyOf(properties) : Map.of();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(Instant modifiedAt) { this.modifiedAt = modifiedAt; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
}
