package io.qamelo.connectivity.infra.net;

import io.qamelo.connectivity.domain.test.ConnectivityTestExecutor;
import io.qamelo.connectivity.domain.test.TestResult;
import io.qamelo.connectivity.domain.test.TestStatus;
import io.smallrye.mutiny.Uni;
import io.vertx.core.net.NetClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.net.NetClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class VertxConnectivityTestExecutor implements ConnectivityTestExecutor {

    @Inject
    Vertx vertx;

    @Override
    public Uni<TestResult> executeTcpConnect(String host, int port, int timeoutSeconds) {
        long startNanos = System.nanoTime();
        NetClientOptions options = new NetClientOptions()
                .setConnectTimeout(timeoutSeconds * 1000);
        NetClient client = vertx.createNetClient(options);
        return client.connect(port, host)
                .map(socket -> {
                    long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                    socket.close();
                    client.close();
                    return new TestResult(TestStatus.SUCCESS, latencyMs, "Connected successfully", null);
                })
                .onFailure().recoverWithItem(err -> {
                    long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                    client.close();
                    if (err.getMessage() != null && err.getMessage().contains("timed out")) {
                        return new TestResult(TestStatus.TIMEOUT, latencyMs, null,
                                "Connection timed out after " + timeoutSeconds + "s");
                    }
                    return new TestResult(TestStatus.FAILED, latencyMs, null, err.getMessage());
                });
    }
}
