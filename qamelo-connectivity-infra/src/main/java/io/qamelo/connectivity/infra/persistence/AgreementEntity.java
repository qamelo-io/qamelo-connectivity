package io.qamelo.connectivity.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agreements")
public class AgreementEntity {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Column(name = "channel_id", nullable = false)
    public UUID channelId;

    @Column(name = "document_type", nullable = false)
    public String documentType;

    @Column(nullable = false)
    public String direction;

    @Column(name = "package_id")
    public String packageId;

    @Column(name = "artifact_id")
    public String artifactId;

    @Column(name = "retry_max_retries")
    public Integer retryMaxRetries;

    @Column(name = "retry_backoff_seconds")
    public Integer retryBackoffSeconds;

    @Column(name = "retry_backoff_multiplier")
    public Double retryBackoffMultiplier;

    @Column(name = "sla_deadline_minutes")
    public Integer slaDeadlineMinutes;

    @Column(name = "pmode_properties", columnDefinition = "TEXT")
    public String pmodeProperties;

    @Column(nullable = false)
    public String status;

    @Version
    public int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "created_by")
    public String createdBy;

    @Column(name = "modified_at")
    public Instant modifiedAt;

    @Column(name = "modified_by")
    public String modifiedBy;
}
