package io.qamelo.connectivity.app.security.iam;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.net.ConnectException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class MtlsIamClient implements IamInternalClient {

    private static final Logger LOG = Logger.getLogger(MtlsIamClient.class);

    @Inject
    @RestClient
    IamInternalRestClient restClient;

    @Override
    public Uni<Optional<IamAuthContextResponse>> fetchAuthContext(String userId) {
        return restClient.getAuthContext(userId)
                .map(Optional::of)
                .onFailure(ConnectException.class).recoverWithItem(e -> {
                    LOG.warnf("IAM unreachable (ConnectException) for user %s: %s", userId, e.getMessage());
                    return Optional.empty();
                })
                .onFailure(TimeoutException.class).recoverWithItem(e -> {
                    LOG.warnf("IAM timeout for user %s: %s", userId, e.getMessage());
                    return Optional.empty();
                });
    }
}
