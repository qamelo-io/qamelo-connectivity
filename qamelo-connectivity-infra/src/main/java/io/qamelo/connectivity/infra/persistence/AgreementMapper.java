package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.agreement.Agreement;
import io.qamelo.connectivity.domain.agreement.AgreementDirection;
import io.qamelo.connectivity.domain.agreement.AgreementStatus;
import io.qamelo.connectivity.domain.agreement.RetryPolicy;

public final class AgreementMapper {

    private AgreementMapper() {}

    public static Agreement toDomain(AgreementEntity entity) {
        RetryPolicy retryPolicy = null;
        if (entity.retryMaxRetries != null) {
            retryPolicy = new RetryPolicy(
                    entity.retryMaxRetries,
                    entity.retryBackoffSeconds != null ? entity.retryBackoffSeconds : 0,
                    entity.retryBackoffMultiplier != null ? entity.retryBackoffMultiplier : 1.0
            );
        }
        return new Agreement(
                entity.id,
                entity.name,
                entity.channelId,
                entity.documentType,
                AgreementDirection.valueOf(entity.direction),
                entity.packageId,
                entity.artifactId,
                retryPolicy,
                entity.slaDeadlineMinutes,
                JsonMapUtil.fromJson(entity.pmodeProperties),
                AgreementStatus.valueOf(entity.status),
                entity.version,
                entity.createdAt,
                entity.createdBy,
                entity.modifiedAt,
                entity.modifiedBy
        );
    }

    public static AgreementEntity toEntity(Agreement agreement) {
        AgreementEntity entity = new AgreementEntity();
        entity.id = agreement.getId();
        entity.name = agreement.getName();
        entity.channelId = agreement.getChannelId();
        entity.documentType = agreement.getDocumentType();
        entity.direction = agreement.getDirection().name();
        entity.packageId = agreement.getPackageId();
        entity.artifactId = agreement.getArtifactId();
        if (agreement.getRetryPolicy() != null) {
            entity.retryMaxRetries = agreement.getRetryPolicy().maxRetries();
            entity.retryBackoffSeconds = agreement.getRetryPolicy().backoffSeconds();
            entity.retryBackoffMultiplier = agreement.getRetryPolicy().backoffMultiplier();
        }
        entity.slaDeadlineMinutes = agreement.getSlaDeadlineMinutes();
        entity.pmodeProperties = JsonMapUtil.toJson(agreement.getPmodeProperties());
        entity.status = agreement.getStatus().name();
        entity.version = agreement.getVersion();
        entity.createdAt = agreement.getCreatedAt();
        entity.createdBy = agreement.getCreatedBy();
        entity.modifiedAt = agreement.getModifiedAt();
        entity.modifiedBy = agreement.getModifiedBy();
        return entity;
    }
}
