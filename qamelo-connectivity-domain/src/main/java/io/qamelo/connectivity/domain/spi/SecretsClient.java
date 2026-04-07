package io.qamelo.connectivity.domain.spi;

import io.smallrye.mutiny.Uni;

import java.util.Map;

/**
 * SPI for writing, reading, and deleting credentials in an external secrets store (Vault).
 * Implementations live in the infra module.
 */
public interface SecretsClient {

    Uni<Void> writeCredential(String path, Map<String, String> data);

    Uni<Map<String, String>> readCredential(String path);

    Uni<Void> deleteCredential(String path);
}
