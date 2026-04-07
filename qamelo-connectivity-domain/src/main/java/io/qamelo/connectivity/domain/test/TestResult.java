package io.qamelo.connectivity.domain.test;

public record TestResult(TestStatus status, long latencyMs, String resultMessage, String errorDetail) {}
