package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.agent.VirtualHost;
import io.qamelo.connectivity.domain.agent.VirtualHostProtocol;
import io.qamelo.connectivity.domain.agent.VirtualHostStatus;

public final class VirtualHostMapper {

    private VirtualHostMapper() {}

    public static VirtualHost toDomain(VirtualHostEntity entity) {
        return new VirtualHost(
                entity.id,
                entity.agentId,
                entity.hostname,
                entity.targetHost,
                entity.targetPort,
                VirtualHostProtocol.valueOf(entity.protocol),
                VirtualHostStatus.valueOf(entity.status),
                entity.connectionId,
                entity.circuitBreakerThreshold,
                entity.timeoutSeconds,
                entity.maxConcurrentConnections,
                entity.retryMaxAttempts,
                entity.retryBackoffMs,
                entity.poolMaxConnections,
                entity.poolKeepAliveSeconds,
                entity.poolIdleTimeoutSeconds,
                entity.createdAt,
                entity.createdBy,
                entity.modifiedAt,
                entity.modifiedBy
        );
    }

    public static VirtualHostEntity toEntity(VirtualHost vh) {
        VirtualHostEntity entity = new VirtualHostEntity();
        entity.id = vh.getId();
        entity.agentId = vh.getAgentId();
        entity.hostname = vh.getHostname();
        entity.targetHost = vh.getTargetHost();
        entity.targetPort = vh.getTargetPort();
        entity.protocol = vh.getProtocol().name();
        entity.status = vh.getStatus().name();
        entity.connectionId = vh.getConnectionId();
        entity.circuitBreakerThreshold = vh.getCircuitBreakerThreshold();
        entity.timeoutSeconds = vh.getTimeoutSeconds();
        entity.maxConcurrentConnections = vh.getMaxConcurrentConnections();
        entity.retryMaxAttempts = vh.getRetryMaxAttempts();
        entity.retryBackoffMs = vh.getRetryBackoffMs();
        entity.poolMaxConnections = vh.getPoolMaxConnections();
        entity.poolKeepAliveSeconds = vh.getPoolKeepAliveSeconds();
        entity.poolIdleTimeoutSeconds = vh.getPoolIdleTimeoutSeconds();
        entity.createdAt = vh.getCreatedAt();
        entity.createdBy = vh.getCreatedBy();
        entity.modifiedAt = vh.getModifiedAt();
        entity.modifiedBy = vh.getModifiedBy();
        return entity;
    }
}
