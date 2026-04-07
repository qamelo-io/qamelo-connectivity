package io.qamelo.connectivity.app.harness;

import io.qamelo.connectivity.domain.spi.SecretsClient;
import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory SecretsClient for integration tests.
 * Replaces the real SecretsHttpClient which calls qamelo-secrets over HTTP.
 */
@Mock
@ApplicationScoped
public class TestSecretsClient implements SecretsClient {

    private final ConcurrentHashMap<String, Map<String, String>> store = new ConcurrentHashMap<>();

    @Override
    public Uni<Void> writeCredential(String path, Map<String, String> data) {
        store.put(path, Map.copyOf(data));
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Map<String, String>> readCredential(String path) {
        Map<String, String> data = store.get(path);
        if (data == null) {
            return Uni.createFrom().item(Map.of());
        }
        return Uni.createFrom().item(data);
    }

    @Override
    public Uni<Void> deleteCredential(String path) {
        store.remove(path);
        return Uni.createFrom().voidItem();
    }

    /** Expose the store for test assertions. */
    public ConcurrentHashMap<String, Map<String, String>> getStore() {
        return store;
    }
}
