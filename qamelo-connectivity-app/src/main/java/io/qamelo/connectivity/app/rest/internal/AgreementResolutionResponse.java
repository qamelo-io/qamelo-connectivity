package io.qamelo.connectivity.app.rest.internal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AgreementResolutionResponse(
        // Agreement
        UUID agreementId,
        String agreementName,
        String documentType,
        String agreementDirection,
        String packageId,
        String artifactId,
        RetryPolicyResponse retryPolicy,
        Integer slaDeadlineMinutes,
        Map<String, String> pmodeProperties,
        String agreementStatus,
        // Channel
        UUID channelId,
        String channelName,
        String channelType,
        String channelDirection,
        Map<String, String> channelProperties,
        boolean channelEnabled,
        // Connection
        UUID connectionId,
        String connectionName,
        String connectionType,
        String connectionHost,
        int connectionPort,
        String connectionAuthType,
        String vaultCredentialPath,
        // Partner
        UUID partnerId,
        String partnerName,
        List<IdentifierEntry> partnerIdentifiers
) {

    public record RetryPolicyResponse(int maxRetries, int backoffSeconds, double backoffMultiplier) {}

    public record IdentifierEntry(String scheme, String customSchemeLabel, String value) {}
}
