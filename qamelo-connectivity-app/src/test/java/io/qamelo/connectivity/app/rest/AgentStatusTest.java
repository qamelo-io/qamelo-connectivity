package io.qamelo.connectivity.app.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AgentStatusTest {

    @Test
    void statusReportUpdatesConnectionHealthToError() {
        // Create connection
        String connectionId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "status-conn-" + UUID.randomUUID(),
                        "type", "REST",
                        "host", "api.example.com",
                        "port", 443,
                        "authType", "API_KEY"
                ))
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Verify connection is ACTIVE
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/connections/" + connectionId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));

        // Create + register agent
        var createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "status-agent-" + UUID.randomUUID()))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract();

        String agentId = createResponse.path("id");
        String token = createResponse.path("registrationToken");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("token", token, "csr", "FAKE_CSR"))
                .when().post("/api/v1/agents/register")
                .then()
                .statusCode(200);

        // Add VH linked to connection
        String hostname = "status-vh-" + UUID.randomUUID();
        Map<String, Object> vhBody = new HashMap<>();
        vhBody.put("hostname", hostname);
        vhBody.put("targetHost", "10.0.0.90");
        vhBody.put("targetPort", 443);
        vhBody.put("connectionId", connectionId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(vhBody)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201);

        // Send status report with unreachable host
        Map<String, Object> statusBody = new HashMap<>();
        statusBody.put("tunnelState", "CONNECTED");
        statusBody.put("remoteAddress", "10.0.0.42:9443");
        statusBody.put("hostHealth", List.of(
                Map.of("hostname", hostname, "reachable", false, "latencyMs", 0)
        ));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(statusBody)
                .when().post("/api/v1/agents/" + agentId + "/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("CONNECTED"));

        // Verify connection status changed to ERROR
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/connections/" + connectionId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ERROR"));
    }

    @Test
    void statusReportRecoverConnectionFromError() {
        // Create connection
        String connectionId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "status-recover-conn-" + UUID.randomUUID(),
                        "type", "REST",
                        "host", "api.example.com",
                        "port", 443,
                        "authType", "API_KEY"
                ))
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create + register agent
        var createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "status-recover-agent-" + UUID.randomUUID()))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract();

        String agentId = createResponse.path("id");
        String token = createResponse.path("registrationToken");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("token", token, "csr", "FAKE_CSR"))
                .when().post("/api/v1/agents/register")
                .then()
                .statusCode(200);

        // Add VH linked to connection
        String hostname = "status-recover-vh-" + UUID.randomUUID();
        Map<String, Object> vhBody = new HashMap<>();
        vhBody.put("hostname", hostname);
        vhBody.put("targetHost", "10.0.0.91");
        vhBody.put("targetPort", 443);
        vhBody.put("connectionId", connectionId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(vhBody)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201);

        // First: Send unreachable to trigger ERROR
        Map<String, Object> errorStatus = new HashMap<>();
        errorStatus.put("tunnelState", "CONNECTED");
        errorStatus.put("remoteAddress", "10.0.0.42:9443");
        errorStatus.put("hostHealth", List.of(
                Map.of("hostname", hostname, "reachable", false, "latencyMs", 0)
        ));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(errorStatus)
                .when().post("/api/v1/agents/" + agentId + "/status")
                .then()
                .statusCode(200);

        // Verify connection is ERROR
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/connections/" + connectionId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ERROR"));

        // Second: Send reachable to recover
        Map<String, Object> recoverStatus = new HashMap<>();
        recoverStatus.put("tunnelState", "CONNECTED");
        recoverStatus.put("remoteAddress", "10.0.0.42:9443");
        recoverStatus.put("hostHealth", List.of(
                Map.of("hostname", hostname, "reachable", true, "latencyMs", 50)
        ));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(recoverStatus)
                .when().post("/api/v1/agents/" + agentId + "/status")
                .then()
                .statusCode(200);

        // Verify connection recovered to ACTIVE
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/connections/" + connectionId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));
    }

    @Test
    void statusReportWithNullHostHealthStillWorks() {
        // Create + register agent
        var createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "status-null-agent-" + UUID.randomUUID()))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract();

        String agentId = createResponse.path("id");
        String token = createResponse.path("registrationToken");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("token", token, "csr", "FAKE_CSR"))
                .when().post("/api/v1/agents/register")
                .then()
                .statusCode(200);

        // Send status report WITHOUT hostHealth (backwards compatible)
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("tunnelState", "CONNECTED", "remoteAddress", "10.0.0.42:9443"))
                .when().post("/api/v1/agents/" + agentId + "/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("CONNECTED"))
                .body("tunnelRemoteAddress", equalTo("10.0.0.42:9443"));
    }
}
