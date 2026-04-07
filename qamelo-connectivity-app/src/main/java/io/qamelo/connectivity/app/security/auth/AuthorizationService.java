package io.qamelo.connectivity.app.security.auth;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import java.util.UUID;

@ApplicationScoped
public class AuthorizationService {

    private final AuthContextCache authContextCache;

    @Inject
    public AuthorizationService(AuthContextCache authContextCache) {
        this.authContextCache = authContextCache;
    }

    public Uni<Boolean> canOperate(String userId, String permission) {
        return authContextCache.getForUser(userId)
                .map(opt -> opt.map(ctx -> ctx.permissions().contains(permission)).orElse(false));
    }

    public Uni<Void> requirePermission(String callerIdProperty, String permission) {
        if (callerIdProperty == null || callerIdProperty.isBlank()) {
            return Uni.createFrom().failure(new ForbiddenException("Missing caller identity"));
        }
        return canOperate(callerIdProperty, permission)
                .chain(allowed -> allowed
                        ? Uni.createFrom().voidItem()
                        : Uni.createFrom().failure(new ForbiddenException(
                        "Missing required permission: " + permission)));
    }
}
