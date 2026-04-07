package io.qamelo.connectivity.infra.secrets;

import io.qamelo.connectivity.domain.error.ConnectivityErrorCode;
import io.qamelo.connectivity.domain.error.ConnectivityException;
import io.qamelo.connectivity.domain.spi.SecretsClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.ConnectException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class SecretsHttpClient implements SecretsClient {

    private static final Logger LOG = Logger.getLogger(SecretsHttpClient.class);
    private static final String KV_PATH_PREFIX = "/api/v1/internal/secrets/kv/";

    private final WebClient webClient;
    private final String secretsUrl;

    SecretsHttpClient(Vertx vertx,
                      @ConfigProperty(name = "qamelo.secrets.url",
                              defaultValue = "http://qamelo-secrets.qamelo-system:9002") String secretsUrl) {
        this.webClient = WebClient.create(vertx);
        this.secretsUrl = secretsUrl;
    }

    @Override
    public Uni<Void> writeCredential(String path, Map<String, String> data) {
        String url = secretsUrl + KV_PATH_PREFIX + path;
        JsonObject body = new JsonObject().put("data", JsonObject.mapFrom(data));
        LOG.debugf("Writing credential to secrets service: path=%s", path);

        return webClient.putAbs(url)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(body)
                .replaceWithVoid()
                .onFailure(SecretsHttpClient::isConnectionFailure)
                .transform(ex -> new ConnectivityException(
                        ConnectivityErrorCode.SERVICE_UNAVAILABLE,
                        "Secrets service unavailable: " + ex.getMessage(), ex));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Uni<Map<String, String>> readCredential(String path) {
        String url = secretsUrl + KV_PATH_PREFIX + path;
        LOG.debugf("Reading credential from secrets service: path=%s", path);

        return webClient.getAbs(url)
                .send()
                .map(response -> {
                    JsonObject json = response.bodyAsJsonObject();
                    JsonObject dataObj = json.getJsonObject("data");
                    return (Map<String, String>) (Map<?, ?>) dataObj.getMap();
                })
                .onFailure(SecretsHttpClient::isConnectionFailure)
                .transform(ex -> new ConnectivityException(
                        ConnectivityErrorCode.SERVICE_UNAVAILABLE,
                        "Secrets service unavailable: " + ex.getMessage(), ex));
    }

    @Override
    public Uni<Void> deleteCredential(String path) {
        String url = secretsUrl + KV_PATH_PREFIX + path;
        LOG.debugf("Deleting credential from secrets service: path=%s", path);

        return webClient.deleteAbs(url)
                .send()
                .replaceWithVoid()
                .onFailure(SecretsHttpClient::isConnectionFailure)
                .transform(ex -> new ConnectivityException(
                        ConnectivityErrorCode.SERVICE_UNAVAILABLE,
                        "Secrets service unavailable: " + ex.getMessage(), ex));
    }

    private static boolean isConnectionFailure(Throwable t) {
        return t instanceof ConnectException || t instanceof TimeoutException;
    }
}
