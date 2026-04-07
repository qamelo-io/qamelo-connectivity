package io.qamelo.connectivity.app.audit;

import io.qamelo.connectivity.app.security.JwtAuthFilter;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.time.Instant;

@Provider
public class AuditFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger AUDIT = Logger.getLogger("io.qamelo.connectivity.audit");
    private static final String START_TIME = "qamelo.audit.startNanos";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty(START_TIME, System.nanoTime());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String path = requestContext.getUriInfo().getPath();
        if (path.startsWith("/q/")) return;

        long startNanos = requestContext.getProperty(START_TIME) != null
                ? (long) requestContext.getProperty(START_TIME) : System.nanoTime();
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

        String userId = requestContext.getProperty(JwtAuthFilter.USER_ID_PROPERTY) != null
                ? requestContext.getProperty(JwtAuthFilter.USER_ID_PROPERTY).toString() : "anonymous";
        String method = requestContext.getMethod();
        int status = responseContext.getStatus();

        AUDIT.infof("{\"ts\":\"%s\",\"user\":\"%s\",\"op\":\"%s %s\",\"status\":%d,\"duration_ms\":%d}",
                Instant.now(), userId, method, path, status, durationMs);
    }
}
