package io.qamelo.connectivity.app.rest;

import io.qamelo.connectivity.app.harness.TestGatewayClient;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class GatewayIntegrationTest {

    @Inject
    TestGatewayClient testGatewayClient;

    @BeforeEach
    void resetGateway() {
        testGatewayClient.reset();
    }

    @Test
    void virtualHostCreatePushesRoutes() {
        // Create + register agent
        var createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "gw-create-agent-" + UUID.randomUUID()))
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

        testGatewayClient.reset(); // clear any incidental pushes

        // Add virtual host
        String hostname = "gw-create-vh-" + UUID.randomUUID();
        Map<String, Object> vhBody = new HashMap<>();
        vhBody.put("hostname", hostname);
        vhBody.put("targetHost", "10.0.0.70");
        vhBody.put("targetPort", 8080);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(vhBody)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201);

        // Verify gateway push happened
        assertThat(testGatewayClient.getPushHistory()).isNotEmpty();
        // Latest push should contain the new VH
        var lastPush = testGatewayClient.getPushHistory()
                .get(testGatewayClient.getPushHistory().size() - 1);
        assertThat(lastPush).anyMatch(e -> e.virtualHostname().equals(hostname));
    }

    @Test
    void virtualHostDeletePushesRoutes() {
        // Create + register agent
        var createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "gw-delete-agent-" + UUID.randomUUID()))
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

        // Add virtual host
        String hostname = "gw-del-vh-" + UUID.randomUUID();
        Map<String, Object> vhBody = new HashMap<>();
        vhBody.put("hostname", hostname);
        vhBody.put("targetHost", "10.0.0.71");
        vhBody.put("targetPort", 8081);

        String vhId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(vhBody)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201)
                .extract().path("id");

        testGatewayClient.reset(); // clear pushes from create

        // Delete virtual host
        given()
                .header("Authorization", "Bearer test-token")
                .when().delete("/api/v1/agents/" + agentId + "/virtual-hosts/" + vhId)
                .then()
                .statusCode(204);

        // Verify gateway push happened after delete
        assertThat(testGatewayClient.getPushHistory()).isNotEmpty();
        // The VH should NOT be in the latest routing table push (it was deleted)
        var lastPush = testGatewayClient.getPushHistory()
                .get(testGatewayClient.getPushHistory().size() - 1);
        assertThat(lastPush).noneMatch(e -> e.virtualHostname().equals(hostname));
    }

    @Test
    void agentDeleteRemovesRoutes() {
        // Create agent (don't register, just PENDING — so we can delete it)
        String agentId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "gw-agent-del-" + UUID.randomUUID()))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract().path("id");

        testGatewayClient.reset();

        // Delete agent
        given()
                .header("Authorization", "Bearer test-token")
                .when().delete("/api/v1/agents/" + agentId)
                .then()
                .statusCode(204);

        // Verify gateway removeRoutes was called for this agent
        assertThat(testGatewayClient.getRemoveHistory()).contains(UUID.fromString(agentId));
    }

    @Test
    void virtualHostUpdatePushesRoutes() {
        // Create + register agent
        var createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "gw-update-agent-" + UUID.randomUUID()))
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

        // Add virtual host
        Map<String, Object> vhBody = new HashMap<>();
        vhBody.put("hostname", "gw-update-vh-" + UUID.randomUUID());
        vhBody.put("targetHost", "10.0.0.72");
        vhBody.put("targetPort", 8082);

        String vhId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(vhBody)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201)
                .extract().path("id");

        testGatewayClient.reset();

        // Update virtual host
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("timeoutSeconds", 60);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/agents/" + agentId + "/virtual-hosts/" + vhId)
                .then()
                .statusCode(200);

        // Verify gateway push happened after update
        assertThat(testGatewayClient.getPushHistory()).isNotEmpty();
    }
}
