package io.qamelo.connectivity.app.rest;

import io.qamelo.connectivity.app.harness.TestKubernetesServiceManager;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class VirtualHostResourceTest {

    @Inject
    TestKubernetesServiceManager testK8sManager;

    @Test
    void addVirtualHost() {
        // Create agent first
        String agentId = createAgent("vh-add-agent-" + UUID.randomUUID());

        String hostname = "vh-add-" + UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("hostname", hostname);
        body.put("targetHost", "10.0.0.50");
        body.put("targetPort", 8080);
        body.put("protocol", "HTTP");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(body)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201)
                .body("hostname", equalTo(hostname))
                .body("targetHost", equalTo("10.0.0.50"))
                .body("targetPort", equalTo(8080))
                .body("protocol", equalTo("HTTP"))
                .body("status", equalTo("ACTIVE"))
                .body("agentId", equalTo(agentId))
                .body("id", notNullValue())
                .body("createdAt", notNullValue())
                // Default resiliency values
                .body("circuitBreakerThreshold", equalTo(5))
                .body("timeoutSeconds", equalTo(30))
                .body("maxConcurrentConnections", equalTo(100))
                .body("retryMaxAttempts", equalTo(3))
                .body("retryBackoffMs", equalTo(1000))
                .body("poolMaxConnections", equalTo(10))
                .body("poolKeepAliveSeconds", equalTo(60))
                .body("poolIdleTimeoutSeconds", equalTo(300));

        // Verify K8s service created
        assertThat(testK8sManager.hasService(hostname)).isTrue();
    }

    @Test
    void listVirtualHosts() {
        String agentId = createAgent("vh-list-agent-" + UUID.randomUUID());

        // Add 2 virtual hosts
        for (int i = 0; i < 2; i++) {
            Map<String, Object> body = new HashMap<>();
            body.put("hostname", "vh-list-" + UUID.randomUUID());
            body.put("targetHost", "10.0.0." + (50 + i));
            body.put("targetPort", 8080 + i);

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer test-token")
                    .body(body)
                    .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                    .then()
                    .statusCode(201);
        }

        // List
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2));
    }

    @Test
    void updateVirtualHost() {
        String agentId = createAgent("vh-update-agent-" + UUID.randomUUID());

        String hostname = "vh-update-" + UUID.randomUUID();
        Map<String, Object> createBody = new HashMap<>();
        createBody.put("hostname", hostname);
        createBody.put("targetHost", "10.0.0.50");
        createBody.put("targetPort", 8080);

        String vhId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(createBody)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Update resiliency config
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("circuitBreakerThreshold", 10);
        updateBody.put("timeoutSeconds", 60);
        updateBody.put("retryMaxAttempts", 5);
        updateBody.put("poolMaxConnections", 20);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/agents/" + agentId + "/virtual-hosts/" + vhId)
                .then()
                .statusCode(200)
                .body("circuitBreakerThreshold", equalTo(10))
                .body("timeoutSeconds", equalTo(60))
                .body("retryMaxAttempts", equalTo(5))
                .body("poolMaxConnections", equalTo(20))
                // Unchanged defaults
                .body("maxConcurrentConnections", equalTo(100))
                .body("retryBackoffMs", equalTo(1000));
    }

    @Test
    void deleteVirtualHost() {
        String agentId = createAgent("vh-delete-agent-" + UUID.randomUUID());

        String hostname = "vh-delete-" + UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("hostname", hostname);
        body.put("targetHost", "10.0.0.50");
        body.put("targetPort", 8080);

        String vhId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(body)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Verify K8s service exists
        assertThat(testK8sManager.hasService(hostname)).isTrue();

        // Delete
        given()
                .header("Authorization", "Bearer test-token")
                .when().delete("/api/v1/agents/" + agentId + "/virtual-hosts/" + vhId)
                .then()
                .statusCode(204);

        // Verify K8s service deleted
        assertThat(testK8sManager.hasService(hostname)).isFalse();
    }

    @Test
    void deleteAgentWithVirtualHosts() {
        String agentId = createAgent("vh-block-agent-" + UUID.randomUUID());

        // Add a virtual host
        Map<String, Object> body = new HashMap<>();
        body.put("hostname", "vh-block-" + UUID.randomUUID());
        body.put("targetHost", "10.0.0.50");
        body.put("targetPort", 8080);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(body)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201);

        // Try to delete agent -> 409
        given()
                .header("Authorization", "Bearer test-token")
                .when().delete("/api/v1/agents/" + agentId)
                .then()
                .statusCode(409)
                .body("error", equalTo("conflict"))
                .body("message", containsString("virtual hosts"));
    }

    @Test
    void deleteAgentWithoutVirtualHosts() {
        String agentId = createAgent("vh-noblock-agent-" + UUID.randomUUID());

        // Delete agent (no VHs) -> 204
        given()
                .header("Authorization", "Bearer test-token")
                .when().delete("/api/v1/agents/" + agentId)
                .then()
                .statusCode(204);

        // Verify gone
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/agents/" + agentId)
                .then()
                .statusCode(404);
    }

    @Test
    void virtualHostWithConnectionId() {
        String agentId = createAgent("vh-conn-agent-" + UUID.randomUUID());

        // Create a connection first
        String connectionId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "vh-conn-" + UUID.randomUUID(),
                        "type", "REST",
                        "host", "api.example.com",
                        "port", 443,
                        "authType", "API_KEY"
                ))
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create VH with connectionId
        Map<String, Object> body = new HashMap<>();
        body.put("hostname", "vh-conn-" + UUID.randomUUID());
        body.put("targetHost", "10.0.0.50");
        body.put("targetPort", 443);
        body.put("connectionId", connectionId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(body)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201)
                .body("connectionId", equalTo(connectionId));
    }

    @Test
    void agentConfigIncludesVirtualHosts() {
        String agentId = createAgent("vh-config-agent-" + UUID.randomUUID());

        // Create a virtual host
        String hostname = "vh-config-" + UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("hostname", hostname);
        body.put("targetHost", "10.0.0.50");
        body.put("targetPort", 8080);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(body)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201);

        // Get agent config — should include virtual hosts
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/agents/" + agentId + "/config")
                .then()
                .statusCode(200)
                .body("agent.id", equalTo(agentId))
                .body("virtualHosts.size()", greaterThanOrEqualTo(1))
                .body("virtualHosts[0].hostname", notNullValue());
    }

    // --- Helper ---

    private String createAgent(String name) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", name))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract().path("id");
    }
}
