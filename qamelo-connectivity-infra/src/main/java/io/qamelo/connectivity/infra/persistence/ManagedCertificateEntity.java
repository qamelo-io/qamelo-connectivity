package io.qamelo.connectivity.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "managed_certificates")
public class ManagedCertificateEntity {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Column(name = "partner_id")
    public UUID partnerId;

    @Column(name = "connection_id")
    public UUID connectionId;

    @Column(name = "agent_id")
    public UUID agentId;

    @Column(nullable = false)
    public String usage;

    @Column(nullable = false)
    public String source;

    @Column(name = "vault_path")
    public String vaultPath;

    @Column(name = "serial_number")
    public String serialNumber;

    @Column(name = "subject_dn")
    public String subjectDn;

    @Column(name = "issuer_dn")
    public String issuerDn;

    @Column(name = "not_before")
    public Instant notBefore;

    @Column(name = "not_after")
    public Instant notAfter;

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
