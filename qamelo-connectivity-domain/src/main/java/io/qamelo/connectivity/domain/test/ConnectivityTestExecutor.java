package io.qamelo.connectivity.domain.test;

import io.smallrye.mutiny.Uni;

public interface ConnectivityTestExecutor {

    Uni<TestResult> executeTcpConnect(String host, int port, int timeoutSeconds);
}
