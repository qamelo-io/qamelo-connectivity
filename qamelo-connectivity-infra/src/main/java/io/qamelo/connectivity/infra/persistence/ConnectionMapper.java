package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.connection.AuthType;
import io.qamelo.connectivity.domain.connection.CertManagement;
import io.qamelo.connectivity.domain.connection.Connection;
import io.qamelo.connectivity.domain.connection.ConnectionStatus;
import io.qamelo.connectivity.domain.connection.ConnectionType;

import java.util.Map;

public final class ConnectionMapper {

    private ConnectionMapper() {}

    public static Connection toDomain(ConnectionEntity entity) {
        return new Connection(
                entity.id,
                entity.name,
                ConnectionType.valueOf(entity.type),
                entity.host,
                entity.port,
                AuthType.valueOf(entity.authType),
                entity.vaultCredentialPath,
                entity.certManagement != null ? CertManagement.valueOf(entity.certManagement) : null,
                entity.vaultClientCertPath,
                entity.vaultTrustStorePath,
                entity.agentId,
                JsonMapUtil.fromJson(entity.properties),
                entity.description,
                ConnectionStatus.valueOf(entity.status),
                entity.createdAt,
                entity.createdBy,
                entity.modifiedAt,
                entity.modifiedBy
        );
    }

    public static ConnectionEntity toEntity(Connection connection) {
        ConnectionEntity entity = new ConnectionEntity();
        entity.id = connection.getId();
        entity.name = connection.getName();
        entity.type = connection.getType().name();
        entity.host = connection.getHost();
        entity.port = connection.getPort();
        entity.authType = connection.getAuthType().name();
        entity.vaultCredentialPath = connection.getVaultCredentialPath();
        entity.certManagement = connection.getCertManagement() != null
                ? connection.getCertManagement().name() : null;
        entity.vaultClientCertPath = connection.getVaultClientCertPath();
        entity.vaultTrustStorePath = connection.getVaultTrustStorePath();
        entity.agentId = connection.getAgentId();
        entity.properties = JsonMapUtil.toJson(connection.getProperties());
        entity.description = connection.getDescription();
        entity.status = connection.getStatus().name();
        entity.createdAt = connection.getCreatedAt();
        entity.createdBy = connection.getCreatedBy();
        entity.modifiedAt = connection.getModifiedAt();
        entity.modifiedBy = connection.getModifiedBy();
        return entity;
    }
}
