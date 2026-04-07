package io.qamelo.connectivity.app.rest.internal;

import io.qamelo.connectivity.app.security.auth.AuthContextCache;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/api/v1/internal/cache")
@Produces(MediaType.APPLICATION_JSON)
public class CacheInvalidationResource {

    @Inject
    AuthContextCache authContextCache;

    @POST
    @Path("/invalidate/user/{userId}")
    public Response invalidateUser(@PathParam("userId") String userId) {
        authContextCache.invalidateUser(userId);
        return Response.ok(Map.of("invalidated", "user:" + userId)).build();
    }

    @POST
    @Path("/invalidate/all")
    public Response invalidateAll() {
        authContextCache.invalidateAll();
        return Response.ok(Map.of("invalidated", "all")).build();
    }
}
