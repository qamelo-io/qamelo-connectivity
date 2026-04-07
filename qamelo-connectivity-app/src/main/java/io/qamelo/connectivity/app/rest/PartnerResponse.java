package io.qamelo.connectivity.app.rest;

import java.util.List;
import java.util.UUID;

public record PartnerResponse(
        UUID id,
        String name,
        String description,
        List<IdentifierResponse> identifiers,
        String createdAt,
        String createdBy,
        String modifiedAt,
        String modifiedBy
) {

    public record IdentifierResponse(
            UUID id,
            String scheme,
            String customSchemeLabel,
            String value
    ) {}
}
