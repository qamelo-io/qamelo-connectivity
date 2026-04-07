package io.qamelo.connectivity.app.rest.internal;

import io.qamelo.connectivity.domain.connection.Connection;
import io.qamelo.connectivity.domain.connection.ConnectionRepository;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/api/v1/internal/connections")
@Produces(MediaType.APPLICATION_JSON)
public class InternalConnectionResource {

    @Inject
    ConnectionRepository connectionRepository;

    @POST
    @Path("/resolve")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<ConnectionResolveResponse> resolve(ConnectionResolveRequest request) {
        List<String> names = request.names() != null ? request.names() : List.of();
        if (names.isEmpty()) {
            return Uni.createFrom().item(new ConnectionResolveResponse(List.of(), List.of()));
        }
        return connectionRepository.findByNames(names)
                .map(connections -> {
                    Set<String> foundNames = connections.stream()
                            .map(Connection::getName)
                            .collect(Collectors.toSet());
                    List<ConnectionResolveResponse.ConnectionResolutionEntry> resolved = connections.stream()
                            .map(InternalConnectionResource::toEntry)
                            .toList();
                    List<String> notFound = new ArrayList<>();
                    for (String name : names) {
                        if (!foundNames.contains(name)) {
                            notFound.add(name);
                        }
                    }
                    return new ConnectionResolveResponse(resolved, notFound);
                });
    }

    private static ConnectionResolveResponse.ConnectionResolutionEntry toEntry(Connection c) {
        return new ConnectionResolveResponse.ConnectionResolutionEntry(
                c.getId(),
                c.getName(),
                c.getType().name(),
                c.getHost(),
                c.getPort(),
                c.getAuthType().name(),
                c.getVaultCredentialPath(),
                c.getCertManagement() != null ? c.getCertManagement().name() : null,
                c.getVaultClientCertPath(),
                c.getVaultTrustStorePath(),
                c.getAgentId(),
                c.getProperties(),
                c.getStatus().name()
        );
    }
}
