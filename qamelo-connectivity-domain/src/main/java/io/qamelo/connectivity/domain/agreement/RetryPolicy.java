package io.qamelo.connectivity.domain.agreement;

public record RetryPolicy(int maxRetries, int backoffSeconds, double backoffMultiplier) {

    public RetryPolicy {
        if (maxRetries < 0) throw new IllegalArgumentException("maxRetries must be >= 0");
        if (backoffSeconds < 0) throw new IllegalArgumentException("backoffSeconds must be >= 0");
        if (backoffMultiplier < 1.0) throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
    }
}
