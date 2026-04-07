package io.qamelo.connectivity.app.rest;

import java.util.Map;
import java.util.UUID;

public record AgreementResponse(
        UUID id,
        String name,
        UUID channelId,
        String documentType,
        String direction,
        String packageId,
        String artifactId,
        RetryPolicyResponse retryPolicy,
        Integer slaDeadlineMinutes,
        Map<String, String> pmodeProperties,
        String status,
        int version,
        String createdAt,
        String createdBy,
        String modifiedAt,
        String modifiedBy
) {

    public record RetryPolicyResponse(int maxRetries, int backoffSeconds, double backoffMultiplier) {}
}
