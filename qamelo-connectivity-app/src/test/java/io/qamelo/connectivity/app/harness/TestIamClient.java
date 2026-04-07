package io.qamelo.connectivity.app.harness;

import io.qamelo.connectivity.app.security.iam.IamAuthContextResponse;
import io.qamelo.connectivity.app.security.iam.IamInternalClient;
import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@Mock
@ApplicationScoped
public class TestIamClient implements IamInternalClient {

    private static final List<String> ALL_PERMISSIONS = List.of(
            "connection:read", "connection:manage",
            "partner:read", "partner:manage",
            "channel:read", "channel:manage",
            "agreement:read", "agreement:manage",
            "agent:read", "agent:manage",
            "connectivity-test:execute"
    );

    @Override
    public Uni<Optional<IamAuthContextResponse>> fetchAuthContext(String userId) {
        if ("test-admin".equals(userId)) {
            var response = new IamAuthContextResponse(
                    userId,
                    ALL_PERMISSIONS,
                    List.of("admin")
            );
            return Uni.createFrom().item(Optional.of(response));
        }
        if ("test-reader".equals(userId)) {
            var response = new IamAuthContextResponse(
                    userId,
                    List.of("connection:read"),
                    List.of("viewer")
            );
            return Uni.createFrom().item(Optional.of(response));
        }
        return Uni.createFrom().item(Optional.empty());
    }
}
