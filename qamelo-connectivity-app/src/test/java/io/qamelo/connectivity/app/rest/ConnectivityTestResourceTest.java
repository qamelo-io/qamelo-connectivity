package io.qamelo.connectivity.app.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ConnectivityTestResourceTest {

    /**
     * Extracts the DevServices PostgreSQL port from the JDBC URL.
     * DevServices injects a URL like "jdbc:postgresql://localhost:49312/default".
     */
    private int getDbPort() {
        String jdbcUrl = ConfigProvider.getConfig()
                .getValue("quarkus.datasource.jdbc.url", String.class);
        // JDBC URL is in form "jdbc:postgresql://host:port/dbname"
        String httpUrl = jdbcUrl.replace("jdbc:postgresql://", "http://");
        URI uri = URI.create(httpUrl);
        return uri.getPort();
    }

    private String getDbHost() {
        String jdbcUrl = ConfigProvider.getConfig()
                .getValue("quarkus.datasource.jdbc.url", String.class);
        String httpUrl = jdbcUrl.replace("jdbc:postgresql://", "http://");
        URI uri = URI.create(httpUrl);
        return uri.getHost();
    }

    private String createConnection(String host, int port, UUID agentId) {
        var body = new HashMap<String, Object>();
        body.put("name", "test-conn-" + UUID.randomUUID());
        body.put("type", "SFTP");
        body.put("host", host);
        body.put("port", port);
        body.put("authType", "BASIC");
        if (agentId != null) {
            body.put("agentId", agentId.toString());
        }

        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(body)
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    void triggerTcpConnectTest() {
        String dbHost = getDbHost();
        int dbPort = getDbPort();
        String connId = createConnection(dbHost, dbPort, null);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .when().post("/api/v1/connections/" + connId + "/test")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("connectionId", equalTo(connId))
                .body("direction", equalTo("CLOUD_TO_REMOTE"))
                .body("type", equalTo("TCP_CONNECT"))
                .body("status", equalTo("SUCCESS"))
                .body("resultMessage", equalTo("Connected successfully"))
                .body("latencyMs", greaterThanOrEqualTo(0))
                .body("completedAt", notNullValue())
                .body("initiatedBy", equalTo("test-admin"));
    }

    @Test
    void triggerTestOnNonexistentConnection() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .when().post("/api/v1/connections/" + UUID.randomUUID() + "/test")
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void triggerTestOnAgentRoutedConnection() {
        String dbHost = getDbHost();
        int dbPort = getDbPort();
        String connId = createConnection(dbHost, dbPort, UUID.randomUUID());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .when().post("/api/v1/connections/" + connId + "/test")
                .then()
                .statusCode(400)
                .body("error", equalTo("bad_request"))
                .body("message", equalTo("Agent-routed connections cannot be tested from cloud side"));
    }

    @Test
    void triggerTestConnectionRefused() {
        String connId = createConnection("localhost", 19999, null);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .when().post("/api/v1/connections/" + connId + "/test")
                .then()
                .statusCode(200)
                .body("status", equalTo("FAILED"))
                .body("errorDetail", notNullValue())
                .body("latencyMs", greaterThanOrEqualTo(0));
    }

    @Test
    void getTestHistory() {
        String dbHost = getDbHost();
        int dbPort = getDbPort();
        String connId = createConnection(dbHost, dbPort, null);

        // Trigger two tests
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .when().post("/api/v1/connections/" + connId + "/test")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .when().post("/api/v1/connections/" + connId + "/test")
                .then()
                .statusCode(200);

        // Get history
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/connections/" + connId + "/tests")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].connectionId", equalTo(connId))
                .body("[1].connectionId", equalTo(connId));
    }

    @Test
    void testUpdatesConnectionStatus() {
        String dbHost = getDbHost();
        int dbPort = getDbPort();

        // Create connection with ERROR status
        var body = new HashMap<String, Object>();
        body.put("name", "status-test-" + UUID.randomUUID());
        body.put("type", "SFTP");
        body.put("host", dbHost);
        body.put("port", dbPort);
        body.put("authType", "BASIC");
        body.put("status", "ERROR");

        String connId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(body)
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .body("status", equalTo("ERROR"))
                .extract().path("id");

        // Trigger successful test
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .when().post("/api/v1/connections/" + connId + "/test")
                .then()
                .statusCode(200)
                .body("status", equalTo("SUCCESS"));

        // Verify connection status is now ACTIVE
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/connections/" + connId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));
    }
}
