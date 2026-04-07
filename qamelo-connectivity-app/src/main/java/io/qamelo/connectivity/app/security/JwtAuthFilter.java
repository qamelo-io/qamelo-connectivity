package io.qamelo.connectivity.app.security;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.util.Set;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(JwtAuthFilter.class);

    public static final String USER_ID_PROPERTY = "qamelo.userId";

    private static final Set<String> BYPASS_PREFIXES = Set.of(
            "/q/health",
            "/api/v1/internal"
    );

    @Inject
    JWTParser jwtParser;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Skip if already handled (e.g. by test filter)
        if (requestContext.getProperty(USER_ID_PROPERTY) != null) {
            return;
        }

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

        try {
            JsonWebToken jwt = jwtParser.parse(token);

            String userId = jwt.getSubject();
            if (userId != null) {
                requestContext.setProperty(USER_ID_PROPERTY, userId);
            }

        } catch (ParseException e) {
            LOG.debugf("JWT validation failed: %s", e.getMessage());
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("{\"error\":\"unauthorized\",\"message\":\"Invalid or expired token\"}")
                            .build());
        }
    }
}
