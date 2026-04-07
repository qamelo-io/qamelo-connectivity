package io.qamelo.connectivity.app.rest;

import java.util.List;

public record PartnerRequest(
        String name,
        String description,
        List<IdentifierRequest> identifiers
) {

    public record IdentifierRequest(
            String scheme,
            String customSchemeLabel,
            String value
    ) {}
}
