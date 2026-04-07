package io.qamelo.connectivity.domain.partner;

import java.util.Objects;
import java.util.UUID;

public final class PartnerIdentifier {

    private final UUID id;
    private final IdentifierScheme scheme;
    private final String customSchemeLabel;
    private final String value;

    public PartnerIdentifier(UUID id, IdentifierScheme scheme, String customSchemeLabel, String value) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        this.scheme = Objects.requireNonNull(scheme, "Scheme must not be null");
        Objects.requireNonNull(value, "Value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Value must not be blank");
        }
        if (scheme == IdentifierScheme.CUSTOM) {
            if (customSchemeLabel == null || customSchemeLabel.isBlank()) {
                throw new IllegalArgumentException("customSchemeLabel is required when scheme is CUSTOM");
            }
        }
        this.customSchemeLabel = customSchemeLabel;
        this.value = value;
    }

    public UUID getId() { return id; }
    public IdentifierScheme getScheme() { return scheme; }
    public String getCustomSchemeLabel() { return customSchemeLabel; }
    public String getValue() { return value; }
}
