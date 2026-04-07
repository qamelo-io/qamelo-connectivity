package io.qamelo.connectivity.domain.agent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Agent {

    private final UUID id;
    private String name;
    private String description;
    private AgentStatus status;
    private String registrationToken;
    private Instant registrationTokenExpiresAt;
    private Instant registeredAt;
    private String certSerialNumber;
    private Instant certExpiresAt;
    private String certSubjectSan;
    private Instant lastSeenAt;
    private String tunnelRemoteAddress;
    private String k8sNamespace;
    private final Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;

    public Agent(UUID id, String name, String description, AgentStatus status,
                 String registrationToken, Instant registrationTokenExpiresAt,
                 Instant registeredAt, String certSerialNumber, Instant certExpiresAt,
                 String certSubjectSan, Instant lastSeenAt, String tunnelRemoteAddress,
                 String k8sNamespace, Instant createdAt, String createdBy,
                 Instant modifiedAt, String modifiedBy) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        setName(name);
        this.description = description;
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.registrationToken = registrationToken;
        this.registrationTokenExpiresAt = registrationTokenExpiresAt;
        this.registeredAt = registeredAt;
        this.certSerialNumber = certSerialNumber;
        this.certExpiresAt = certExpiresAt;
        this.certSubjectSan = certSubjectSan;
        this.lastSeenAt = lastSeenAt;
        this.tunnelRemoteAddress = tunnelRemoteAddress;
        this.k8sNamespace = k8sNamespace;
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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public AgentStatus getStatus() { return status; }
    public void setStatus(AgentStatus status) { this.status = Objects.requireNonNull(status); }

    public String getRegistrationToken() { return registrationToken; }
    public void setRegistrationToken(String registrationToken) { this.registrationToken = registrationToken; }

    public Instant getRegistrationTokenExpiresAt() { return registrationTokenExpiresAt; }
    public void setRegistrationTokenExpiresAt(Instant registrationTokenExpiresAt) { this.registrationTokenExpiresAt = registrationTokenExpiresAt; }

    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }

    public String getCertSerialNumber() { return certSerialNumber; }
    public void setCertSerialNumber(String certSerialNumber) { this.certSerialNumber = certSerialNumber; }

    public Instant getCertExpiresAt() { return certExpiresAt; }
    public void setCertExpiresAt(Instant certExpiresAt) { this.certExpiresAt = certExpiresAt; }

    public String getCertSubjectSan() { return certSubjectSan; }
    public void setCertSubjectSan(String certSubjectSan) { this.certSubjectSan = certSubjectSan; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public String getTunnelRemoteAddress() { return tunnelRemoteAddress; }
    public void setTunnelRemoteAddress(String tunnelRemoteAddress) { this.tunnelRemoteAddress = tunnelRemoteAddress; }

    public String getK8sNamespace() { return k8sNamespace; }
    public void setK8sNamespace(String k8sNamespace) { this.k8sNamespace = k8sNamespace; }

    public Instant getCreatedAt() { return createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(Instant modifiedAt) { this.modifiedAt = modifiedAt; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
}
