package io.qamelo.connectivity.app.security.iam;

import io.smallrye.mutiny.Uni;

import java.util.Optional;

public interface IamInternalClient {

    Uni<Optional<IamAuthContextResponse>> fetchAuthContext(String userId);
}
