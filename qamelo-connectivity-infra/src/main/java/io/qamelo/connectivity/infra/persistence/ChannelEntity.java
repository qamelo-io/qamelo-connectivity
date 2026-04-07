package io.qamelo.connectivity.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "channels")
public class ChannelEntity {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Column(name = "partner_id", nullable = false)
    public UUID partnerId;

    @Column(name = "connection_id", nullable = false)
    public UUID connectionId;

    @Column(nullable = false)
    public String type;

    @Column(nullable = false)
    public String direction;

    @Column(columnDefinition = "TEXT")
    public String properties;

    @Column(nullable = false)
    public boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "created_by")
    public String createdBy;

    @Column(name = "modified_at")
    public Instant modifiedAt;

    @Column(name = "modified_by")
    public String modifiedBy;
}
