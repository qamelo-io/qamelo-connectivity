package io.qamelo.connectivity.domain.partner;

import java.util.regex.Pattern;

/**
 * Domain service that validates partner identifiers against per-scheme rules.
 */
public final class IdentifierValidator {

    private static final Pattern DIGITS_ONLY = Pattern.compile("\\d+");
    private static final Pattern ALPHANUMERIC = Pattern.compile("[A-Za-z0-9]+");

    private IdentifierValidator() {}

    /**
     * Validates a partner identifier according to its scheme rules.
     *
     * @throws IllegalArgumentException if the identifier fails validation
     */
    public static void validate(PartnerIdentifier identifier) {
        switch (identifier.getScheme()) {
            case DUNS -> validateDuns(identifier.getValue());
            case GLN -> validateGln(identifier.getValue());
            case ICO -> validateIco(identifier.getValue());
            case EIC -> validateEic(identifier.getValue());
            case VAT -> validateNonBlank(identifier.getValue(), "VAT");
            case LEI -> validateNonBlank(identifier.getValue(), "LEI");
            case CUSTOM -> validateCustom(identifier);
        }
    }

    private static void validateDuns(String value) {
        if (value.length() != 9 || !DIGITS_ONLY.matcher(value).matches()) {
            throw new IllegalArgumentException("DUNS must be exactly 9 digits, got: " + value);
        }
    }

    private static void validateGln(String value) {
        if (value.length() != 13 || !DIGITS_ONLY.matcher(value).matches()) {
            throw new IllegalArgumentException("GLN must be exactly 13 digits, got: " + value);
        }
    }

    private static void validateIco(String value) {
        if (value.length() != 8 || !DIGITS_ONLY.matcher(value).matches()) {
            throw new IllegalArgumentException("ICO must be exactly 8 digits, got: " + value);
        }
    }

    private static void validateEic(String value) {
        if (value.length() != 16 || !ALPHANUMERIC.matcher(value).matches()) {
            throw new IllegalArgumentException("EIC must be exactly 16 alphanumeric characters, got: " + value);
        }
    }

    private static void validateNonBlank(String value, String schemeName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(schemeName + " value must not be blank");
        }
    }

    private static void validateCustom(PartnerIdentifier identifier) {
        if (identifier.getCustomSchemeLabel() == null || identifier.getCustomSchemeLabel().isBlank()) {
            throw new IllegalArgumentException("customSchemeLabel is required when scheme is CUSTOM");
        }
        validateNonBlank(identifier.getValue(), "CUSTOM");
    }
}
