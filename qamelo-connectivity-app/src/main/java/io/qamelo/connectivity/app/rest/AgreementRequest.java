package io.qamelo.connectivity.app.rest;

import java.util.Map;

public record AgreementRequest(
        String name,
        String channelId,
        String documentType,
        String direction,
        String packageId,
        String artifactId,
        RetryPolicyRequest retryPolicy,
        Integer slaDeadlineMinutes,
        Map<String, String> pmodeProperties,
        String status,
        Integer version
) {

    public record RetryPolicyRequest(int maxRetries, int backoffSeconds, double backoffMultiplier) {}
}
