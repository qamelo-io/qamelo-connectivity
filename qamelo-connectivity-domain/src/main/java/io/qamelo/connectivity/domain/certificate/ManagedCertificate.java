package io.qamelo.connectivity.domain.certificate;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ManagedCertificate {

    private final UUID id;
    private String name;
    private UUID partnerId;
    private UUID connectionId;
    private UUID agentId;
    private CertificateUsage usage;
    private CertificateSource source;
    private String vaultPath;
    private String serialNumber;
    private String subjectDn;
    private String issuerDn;
    private Instant notBefore;
    private Instant notAfter;
    private CertificateStatus status;
    private int version;
    private final Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;

    public ManagedCertificate(UUID id, String name, UUID partnerId, UUID connectionId, UUID agentId,
                              CertificateUsage usage, CertificateSource source,
                              String vaultPath, String serialNumber, String subjectDn, String issuerDn,
                              Instant notBefore, Instant notAfter, CertificateStatus status, int version,
                              Instant createdAt, String createdBy, Instant modifiedAt, String modifiedBy) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        setName(name);
        this.usage = Objects.requireNonNull(usage, "Usage must not be null");
        this.source = Objects.requireNonNull(source, "Source must not be null");
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt must not be null");

        // Validate exactly one FK is set
        int fkCount = (partnerId != null ? 1 : 0) + (connectionId != null ? 1 : 0) + (agentId != null ? 1 : 0);
        if (fkCount != 1) {
            throw new IllegalArgumentException("Exactly one of partnerId, connectionId, or agentId must be set");
        }
        this.partnerId = partnerId;
        this.connectionId = connectionId;
        this.agentId = agentId;

        this.vaultPath = vaultPath;
        this.serialNumber = serialNumber;
        this.subjectDn = subjectDn;
        this.issuerDn = issuerDn;
        this.notBefore = notBefore;
        this.notAfter = notAfter;
        this.version = version;
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
    public void setPartnerId(UUID partnerId) { this.partnerId = partnerId; }

    public UUID getConnectionId() { return connectionId; }
    public void setConnectionId(UUID connectionId) { this.connectionId = connectionId; }

    public UUID getAgentId() { return agentId; }
    public void setAgentId(UUID agentId) { this.agentId = agentId; }

    public CertificateUsage getUsage() { return usage; }
    public void setUsage(CertificateUsage usage) { this.usage = Objects.requireNonNull(usage); }

    public CertificateSource getSource() { return source; }
    public void setSource(CertificateSource source) { this.source = Objects.requireNonNull(source); }

    public String getVaultPath() { return vaultPath; }
    public void setVaultPath(String vaultPath) { this.vaultPath = vaultPath; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getSubjectDn() { return subjectDn; }
    public void setSubjectDn(String subjectDn) { this.subjectDn = subjectDn; }

    public String getIssuerDn() { return issuerDn; }
    public void setIssuerDn(String issuerDn) { this.issuerDn = issuerDn; }

    public Instant getNotBefore() { return notBefore; }
    public void setNotBefore(Instant notBefore) { this.notBefore = notBefore; }

    public Instant getNotAfter() { return notAfter; }
    public void setNotAfter(Instant notAfter) { this.notAfter = notAfter; }

    public CertificateStatus getStatus() { return status; }
    public void setStatus(CertificateStatus status) { this.status = Objects.requireNonNull(status); }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(Instant modifiedAt) { this.modifiedAt = modifiedAt; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
}
