package io.qamelo.connectivity.app.security.auth;

import io.smallrye.mutiny.Uni;

import java.util.Optional;

public interface AuthContextCache {

    Uni<Optional<UserAuthContext>> getForUser(String userId);

    void invalidateUser(String userId);

    void invalidateAll();
}
