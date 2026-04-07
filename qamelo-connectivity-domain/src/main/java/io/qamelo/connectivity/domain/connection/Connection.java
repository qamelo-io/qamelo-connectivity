package io.qamelo.connectivity.domain.connection;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class Connection {

    private final UUID id;
    private String name;
    private ConnectionType type;
    private String host;
    private int port;
    private AuthType authType;
    private String vaultCredentialPath;
    private CertManagement certManagement;
    private String vaultClientCertPath;
    private String vaultTrustStorePath;
    private UUID agentId;
    private Map<String, String> properties;
    private String description;
    private ConnectionStatus status;
    private final Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;

    public Connection(UUID id, String name, ConnectionType type, String host, int port,
                      AuthType authType, String vaultCredentialPath, CertManagement certManagement,
                      String vaultClientCertPath, String vaultTrustStorePath, UUID agentId,
                      Map<String, String> properties, String description, ConnectionStatus status,
                      Instant createdAt, String createdBy, Instant modifiedAt, String modifiedBy) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        setName(name);
        this.type = Objects.requireNonNull(type, "Type must not be null");
        this.host = Objects.requireNonNull(host, "Host must not be null");
        this.port = port;
        this.authType = Objects.requireNonNull(authType, "AuthType must not be null");
        this.vaultCredentialPath = vaultCredentialPath;
        this.certManagement = certManagement;
        this.vaultClientCertPath = vaultClientCertPath;
        this.vaultTrustStorePath = vaultTrustStorePath;
        this.agentId = agentId;
        this.properties = properties != null ? Map.copyOf(properties) : Map.of();
        this.description = description;
        this.status = Objects.requireNonNull(status, "Status must not be null");
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

    public ConnectionType getType() { return type; }
    public void setType(ConnectionType type) { this.type = Objects.requireNonNull(type); }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = Objects.requireNonNull(host); }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public AuthType getAuthType() { return authType; }
    public void setAuthType(AuthType authType) { this.authType = Objects.requireNonNull(authType); }

    public String getVaultCredentialPath() { return vaultCredentialPath; }
    public void setVaultCredentialPath(String vaultCredentialPath) { this.vaultCredentialPath = vaultCredentialPath; }

    public CertManagement getCertManagement() { return certManagement; }
    public void setCertManagement(CertManagement certManagement) { this.certManagement = certManagement; }

    public String getVaultClientCertPath() { return vaultClientCertPath; }
    public void setVaultClientCertPath(String vaultClientCertPath) { this.vaultClientCertPath = vaultClientCertPath; }

    public String getVaultTrustStorePath() { return vaultTrustStorePath; }
    public void setVaultTrustStorePath(String vaultTrustStorePath) { this.vaultTrustStorePath = vaultTrustStorePath; }

    public UUID getAgentId() { return agentId; }
    public void setAgentId(UUID agentId) { this.agentId = agentId; }

    public Map<String, String> getProperties() { return properties; }
    public void setProperties(Map<String, String> properties) {
        this.properties = properties != null ? Map.copyOf(properties) : Map.of();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ConnectionStatus getStatus() { return status; }
    public void setStatus(ConnectionStatus status) { this.status = Objects.requireNonNull(status); }

    public Instant getCreatedAt() { return createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(Instant modifiedAt) { this.modifiedAt = modifiedAt; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
}
