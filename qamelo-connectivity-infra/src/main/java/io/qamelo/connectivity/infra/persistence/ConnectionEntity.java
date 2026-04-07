package io.qamelo.connectivity.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connections")
public class ConnectionEntity {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String type;

    @Column(nullable = false)
    public String host;

    @Column(nullable = false)
    public int port;

    @Column(name = "auth_type", nullable = false)
    public String authType;

    @Column(name = "vault_credential_path")
    public String vaultCredentialPath;

    @Column(name = "cert_management")
    public String certManagement;

    @Column(name = "vault_client_cert_path")
    public String vaultClientCertPath;

    @Column(name = "vault_trust_store_path")
    public String vaultTrustStorePath;

    @Column(name = "agent_id")
    public UUID agentId;

    @Column(columnDefinition = "TEXT")
    public String properties;

    public String description;

    @Column(nullable = false)
    public String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "created_by")
    public String createdBy;

    @Column(name = "modified_at")
    public Instant modifiedAt;

    @Column(name = "modified_by")
    public String modifiedBy;
}
