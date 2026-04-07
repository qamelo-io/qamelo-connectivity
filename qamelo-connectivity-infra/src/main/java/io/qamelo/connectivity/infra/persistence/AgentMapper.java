package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.agent.Agent;
import io.qamelo.connectivity.domain.agent.AgentStatus;

public final class AgentMapper {

    private AgentMapper() {}

    public static Agent toDomain(AgentEntity entity) {
        return new Agent(
                entity.id,
                entity.name,
                entity.description,
                AgentStatus.valueOf(entity.status),
                entity.registrationToken,
                entity.registrationTokenExpiresAt,
                entity.registeredAt,
                entity.certSerialNumber,
                entity.certExpiresAt,
                entity.certSubjectSan,
                entity.lastSeenAt,
                entity.tunnelRemoteAddress,
                entity.k8sNamespace,
                entity.createdAt,
                entity.createdBy,
                entity.modifiedAt,
                entity.modifiedBy
        );
    }

    public static AgentEntity toEntity(Agent agent) {
        AgentEntity entity = new AgentEntity();
        entity.id = agent.getId();
        entity.name = agent.getName();
        entity.description = agent.getDescription();
        entity.status = agent.getStatus().name();
        entity.registrationToken = agent.getRegistrationToken();
        entity.registrationTokenExpiresAt = agent.getRegistrationTokenExpiresAt();
        entity.registeredAt = agent.getRegisteredAt();
        entity.certSerialNumber = agent.getCertSerialNumber();
        entity.certExpiresAt = agent.getCertExpiresAt();
        entity.certSubjectSan = agent.getCertSubjectSan();
        entity.lastSeenAt = agent.getLastSeenAt();
        entity.tunnelRemoteAddress = agent.getTunnelRemoteAddress();
        entity.k8sNamespace = agent.getK8sNamespace();
        entity.createdAt = agent.getCreatedAt();
        entity.createdBy = agent.getCreatedBy();
        entity.modifiedAt = agent.getModifiedAt();
        entity.modifiedBy = agent.getModifiedBy();
        return entity;
    }
}
