package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.partner.IdentifierScheme;
import io.qamelo.connectivity.domain.partner.Partner;
import io.qamelo.connectivity.domain.partner.PartnerIdentifier;

import java.util.List;

public final class PartnerMapper {

    private PartnerMapper() {}

    public static Partner toDomain(PartnerEntity entity, List<PartnerIdentifierEntity> identifierEntities) {
        List<PartnerIdentifier> identifiers = identifierEntities.stream()
                .map(PartnerMapper::toDomainIdentifier)
                .toList();
        return new Partner(
                entity.id,
                entity.name,
                entity.description,
                identifiers,
                entity.createdAt,
                entity.createdBy,
                entity.modifiedAt,
                entity.modifiedBy
        );
    }

    public static PartnerEntity toEntity(Partner partner) {
        PartnerEntity entity = new PartnerEntity();
        entity.id = partner.getId();
        entity.name = partner.getName();
        entity.description = partner.getDescription();
        entity.createdAt = partner.getCreatedAt();
        entity.createdBy = partner.getCreatedBy();
        entity.modifiedAt = partner.getModifiedAt();
        entity.modifiedBy = partner.getModifiedBy();
        return entity;
    }

    public static List<PartnerIdentifierEntity> toIdentifierEntities(Partner partner) {
        return partner.getIdentifiers().stream()
                .map(id -> toIdentifierEntity(id, partner.getId()))
                .toList();
    }

    private static PartnerIdentifier toDomainIdentifier(PartnerIdentifierEntity entity) {
        return new PartnerIdentifier(
                entity.id,
                IdentifierScheme.valueOf(entity.scheme),
                entity.customSchemeLabel,
                entity.value
        );
    }

    private static PartnerIdentifierEntity toIdentifierEntity(PartnerIdentifier identifier, java.util.UUID partnerId) {
        PartnerIdentifierEntity entity = new PartnerIdentifierEntity();
        entity.id = identifier.getId();
        entity.partnerId = partnerId;
        entity.scheme = identifier.getScheme().name();
        entity.customSchemeLabel = identifier.getCustomSchemeLabel();
        entity.value = identifier.getValue();
        return entity;
    }
}
