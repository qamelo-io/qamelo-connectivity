package io.qamelo.connectivity.domain.partner;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface PartnerRepository {

    Uni<Partner> findById(UUID id);

    Multi<Partner> findAll();

    Uni<Partner> findByIdentifier(IdentifierScheme scheme, String value);

    Uni<Partner> save(Partner partner);

    Uni<Partner> update(Partner partner);

    Uni<Void> delete(UUID id);

    Uni<Boolean> hasChannels(UUID partnerId);
}
