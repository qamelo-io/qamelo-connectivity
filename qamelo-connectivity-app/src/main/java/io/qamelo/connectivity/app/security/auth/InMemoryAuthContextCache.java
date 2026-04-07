package io.qamelo.connectivity.app.security.auth;

import io.qamelo.connectivity.app.security.iam.IamAuthContextResponse;
import io.qamelo.connectivity.app.security.iam.IamInternalClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InMemoryAuthContextCache implements AuthContextCache {

    private static final Logger LOG = Logger.getLogger(InMemoryAuthContextCache.class);

    @Inject
    IamInternalClient iamClient;

    @ConfigProperty(name = "qamelo.auth.cache.user-ttl-seconds", defaultValue = "60")
    long userTtlSeconds;

    private final ConcurrentHashMap<String, CacheEntry<UserAuthContext>> userCache = new ConcurrentHashMap<>();

    @Override
    public Uni<Optional<UserAuthContext>> getForUser(String userId) {
        CacheEntry<UserAuthContext> existing = userCache.get(userId);

        if (existing != null && !isStale(existing.fetchedAt())) {
            return Uni.createFrom().item(Optional.of(existing.value()));
        }

        return iamClient.fetchAuthContext(userId)
                .map(opt -> {
                    if (opt.isPresent()) {
                        UserAuthContext ctx = toUserAuthContext(opt.get());
                        userCache.put(userId, new CacheEntry<>(ctx, Instant.now()));
                        return Optional.of(ctx);
                    } else if (existing != null) {
                        LOG.warnf("IAM unreachable; serving stale auth context for user %s", userId);
                        return Optional.of(existing.value());
                    } else {
                        return Optional.<UserAuthContext>empty();
                    }
                });
    }

    @Override
    public void invalidateUser(String userId) {
        userCache.remove(userId);
    }

    @Override
    public void invalidateAll() {
        userCache.clear();
    }

    private boolean isStale(Instant fetchedAt) {
        return fetchedAt.plusSeconds(userTtlSeconds).isBefore(Instant.now());
    }

    private UserAuthContext toUserAuthContext(IamAuthContextResponse response) {
        Set<String> permissions = response.permissions() != null
                ? new HashSet<>(response.permissions())
                : Set.of();
        return new UserAuthContext(response.userId(), permissions);
    }

    record CacheEntry<T>(T value, Instant fetchedAt) {}
}
