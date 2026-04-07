package io.qamelo.connectivity.app.rest;

public record CertificateRequest(
        String name,
        String usage,
        String source,
        String vaultPath,
        String serialNumber,
        String subjectDn,
        String issuerDn,
        String notBefore,
        String notAfter
) {}
