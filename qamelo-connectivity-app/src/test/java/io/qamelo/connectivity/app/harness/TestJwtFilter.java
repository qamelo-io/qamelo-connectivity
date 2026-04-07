package io.qamelo.connectivity.app.harness;

import io.qamelo.connectivity.app.security.JwtAuthFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;
import java.util.Set;

/**
 * Test filter that runs BEFORE the real JwtAuthFilter (higher priority = lower number).
 * Maps test tokens to user IDs so integration tests don't need real JWT infrastructure.
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 5)
public class TestJwtFilter implements ContainerRequestFilter {

    private static final Map<String, String> TOKEN_MAP = Map.of(
            "test-token", "test-admin",
            "reader-token", "test-reader"
    );

    private static final Set<String> BYPASS_PREFIXES = Set.of(
            "/q/health",
            "/api/v1/internal"
    );

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();

        for (String prefix : BYPASS_PREFIXES) {
            if (path.startsWith(prefix)) {
                return;
            }
        }

        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("{\"error\":\"unauthorized\",\"message\":\"Missing or invalid Authorization header\"}")
                            .build());
            return;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        String userId = TOKEN_MAP.get(token);

        if (userId == null) {
            requestContext.abortWith(
                    Response.status(Response.Status.FORBIDDEN)
                            .entity("{\"error\":\"forbidden\",\"message\":\"Unknown test token\"}")
                            .build());
            return;
        }

        requestContext.setProperty(JwtAuthFilter.USER_ID_PROPERTY, userId);
        // Mark as already handled so JwtAuthFilter skips
        requestContext.setProperty("qamelo.auth.handled", true);
    }
}
