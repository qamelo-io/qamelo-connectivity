package io.qamelo.connectivity.domain.spi;

import java.time.Instant;

public record PkiSignResponse(String certificate, String caChain, String serialNumber, Instant expiration) {}
