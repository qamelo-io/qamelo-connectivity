package io.qamelo.connectivity.domain.spi;

import io.smallrye.mutiny.Uni;

import java.util.Map;

/**
 * SPI for writing, reading, and deleting credentials in an external secrets store (Vault),
 * and for PKI certificate operations (sign CSR, revoke certificate).
 * Implementations live in the infra module.
 */
public interface SecretsClient {

    Uni<Void> writeCredential(String path, Map<String, String> data);

    Uni<Map<String, String>> readCredential(String path);

    Uni<Void> deleteCredential(String path);

    // PKI operations

    Uni<PkiSignResponse> signCsr(String pkiMount, String role, String csr,
                                  String commonName, String subjectAltName, String ttl);

    Uni<Void> revokeCertificate(String pkiMount, String serialNumber);
}
