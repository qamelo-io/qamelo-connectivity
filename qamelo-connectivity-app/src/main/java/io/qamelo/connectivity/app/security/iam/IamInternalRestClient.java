package io.qamelo.connectivity.app.security.iam;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "iam-internal")
@Path("/api/v1/internal")
public interface IamInternalRestClient {

    @GET
    @Path("/auth-context/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    Uni<IamAuthContextResponse> getAuthContext(@PathParam("userId") String userId);
}
