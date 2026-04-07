package io.qamelo.connectivity.app.harness;

import io.qamelo.connectivity.domain.spi.PkiSignResponse;
import io.qamelo.connectivity.domain.spi.SecretsClient;
import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory SecretsClient for integration tests.
 * Replaces the real SecretsHttpClient which calls qamelo-secrets over HTTP.
 */
@Mock
@ApplicationScoped
public class TestSecretsClient implements SecretsClient {

    private final ConcurrentHashMap<String, Map<String, String>> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> pkiCerts = new ConcurrentHashMap<>();

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

    // PKI operations

    @Override
    public Uni<PkiSignResponse> signCsr(String pkiMount, String role, String csr,
                                         String commonName, String subjectAltName, String ttl) {
        String serial = UUID.randomUUID().toString();
        Instant expiration = Instant.now().plus(90, ChronoUnit.DAYS);
        String fakeCert = "-----BEGIN CERTIFICATE-----\nMOCK_CERT_" + serial + "\n-----END CERTIFICATE-----";
        String fakeChain = "-----BEGIN CERTIFICATE-----\nMOCK_CA_CHAIN\n-----END CERTIFICATE-----";
        pkiCerts.put(serial, fakeCert);
        return Uni.createFrom().item(new PkiSignResponse(fakeCert, fakeChain, serial, expiration));
    }

    @Override
    public Uni<Void> revokeCertificate(String pkiMount, String serialNumber) {
        pkiCerts.remove(serialNumber);
        return Uni.createFrom().voidItem();
    }

    /** Expose the PKI cert store for test assertions. */
    public ConcurrentHashMap<String, String> getPkiCerts() {
        return pkiCerts;
    }
}
