package io.qamelo.connectivity.domain.agreement;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface AgreementRepository {

    Uni<Agreement> findById(UUID id);

    Multi<Agreement> findAll();

    Multi<Agreement> findByChannelId(UUID channelId);

    Uni<Agreement> save(Agreement agreement);

    Uni<Agreement> update(Agreement agreement);

    Uni<Void> delete(UUID id);
}
