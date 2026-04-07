package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.certificate.CertificateSource;
import io.qamelo.connectivity.domain.certificate.CertificateStatus;
import io.qamelo.connectivity.domain.certificate.CertificateUsage;
import io.qamelo.connectivity.domain.certificate.ManagedCertificate;

public final class ManagedCertificateMapper {

    private ManagedCertificateMapper() {}

    public static ManagedCertificate toDomain(ManagedCertificateEntity entity) {
        return new ManagedCertificate(
                entity.id,
                entity.name,
                entity.partnerId,
                entity.connectionId,
                entity.agentId,
                CertificateUsage.valueOf(entity.usage),
                CertificateSource.valueOf(entity.source),
                entity.vaultPath,
                entity.serialNumber,
                entity.subjectDn,
                entity.issuerDn,
                entity.notBefore,
                entity.notAfter,
                CertificateStatus.valueOf(entity.status),
                entity.version,
                entity.createdAt,
                entity.createdBy,
                entity.modifiedAt,
                entity.modifiedBy
        );
    }

    public static ManagedCertificateEntity toEntity(ManagedCertificate cert) {
        ManagedCertificateEntity entity = new ManagedCertificateEntity();
        entity.id = cert.getId();
        entity.name = cert.getName();
        entity.partnerId = cert.getPartnerId();
        entity.connectionId = cert.getConnectionId();
        entity.agentId = cert.getAgentId();
        entity.usage = cert.getUsage().name();
        entity.source = cert.getSource().name();
        entity.vaultPath = cert.getVaultPath();
        entity.serialNumber = cert.getSerialNumber();
        entity.subjectDn = cert.getSubjectDn();
        entity.issuerDn = cert.getIssuerDn();
        entity.notBefore = cert.getNotBefore();
        entity.notAfter = cert.getNotAfter();
        entity.status = cert.getStatus().name();
        entity.version = cert.getVersion();
        entity.createdAt = cert.getCreatedAt();
        entity.createdBy = cert.getCreatedBy();
        entity.modifiedAt = cert.getModifiedAt();
        entity.modifiedBy = cert.getModifiedBy();
        return entity;
    }
}
