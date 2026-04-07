package io.qamelo.connectivity.app.rest;

import io.qamelo.connectivity.app.harness.TestSecretsClient;
import io.qamelo.connectivity.domain.agent.AgentRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AgentResourceTest {

    @Inject
    TestSecretsClient testSecretsClient;

    @Inject
    AgentRepository agentRepository;

    @Test
    void createAgent() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "test-agent-" + UUID.randomUUID(),
                        "description", "Test agent"
                ))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .body("status", equalTo("PENDING"))
                .body("registrationToken", notNullValue())
                .body("registrationToken", hasLength(64)) // 256 bits = 32 bytes = 64 hex chars
                .body("registrationTokenExpiresAt", notNullValue())
                .body("id", notNullValue())
                .body("createdAt", notNullValue());
    }

    @Test
    void getAgent() {
        // Create
        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "get-agent-" + UUID.randomUUID(),
                        "description", "Get test agent",
                        "k8sNamespace", "test-ns"
                ))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Read back
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/agents/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("status", equalTo("PENDING"))
                .body("description", equalTo("Get test agent"))
                .body("k8sNamespace", equalTo("test-ns"));
    }

    @Test
    void listAgents() {
        // Create two agents
        for (int i = 0; i < 2; i++) {
            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer test-token")
                    .body(Map.of("name", "list-agent-" + UUID.randomUUID()))
                    .when().post("/api/v1/agents")
                    .then()
                    .statusCode(201);
        }

        // List all
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/agents")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2));
    }

    @Test
    void updateAgent() {
        // Create
        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "update-agent-" + UUID.randomUUID()))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Update
        String newName = "updated-agent-" + UUID.randomUUID();
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", newName,
                        "description", "Updated description",
                        "k8sNamespace", "new-ns"
                ))
                .when().put("/api/v1/agents/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo(newName))
                .body("description", equalTo("Updated description"))
                .body("k8sNamespace", equalTo("new-ns"));
    }

    @Test
    void deleteAgent() {
        // Create
        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "delete-agent-" + UUID.randomUUID()))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Delete
        given()
                .header("Authorization", "Bearer test-token")
                .when().delete("/api/v1/agents/" + id)
                .then()
                .statusCode(204);

        // Verify gone
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/agents/" + id)
                .then()
                .statusCode(404);
    }

    @Test
    void regenerateToken() {
        // Create
        String originalToken = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "regen-agent-" + UUID.randomUUID()))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract().path("registrationToken");

        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "regen-agent2-" + UUID.randomUUID()))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Let me re-create to get both id and token
        String name = "regen-agent3-" + UUID.randomUUID();
        var response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", name))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract();

        String agentId = response.path("id");
        String firstToken = response.path("registrationToken");

        // Regenerate
        String newToken = given()
                .header("Authorization", "Bearer test-token")
                .when().post("/api/v1/agents/" + agentId + "/regenerate-token")
                .then()
                .statusCode(200)
                .body("registrationToken", notNullValue())
                .body("registrationToken", hasLength(64))
                .extract().path("registrationToken");

        assertThat(newToken).isNotEqualTo(firstToken);
    }

    @Test
    void registerAgent() {
        // Create agent
        var createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "register-agent-" + UUID.randomUUID()))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract();

        String agentId = createResponse.path("id");
        String token = createResponse.path("registrationToken");

        // Register with token + CSR (no auth header needed)
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("token", token, "csr", "-----BEGIN CERTIFICATE REQUEST-----\nFAKE_CSR\n-----END CERTIFICATE REQUEST-----"))
                .when().post("/api/v1/agents/register")
                .then()
                .statusCode(200)
                .body("certificate", notNullValue())
                .body("certificate", startsWith("-----BEGIN CERTIFICATE-----"))
                .body("caChain", notNullValue())
                .body("gatewayUrl", equalTo("https://gateway.qamelo.io"));

        // Verify agent status changed
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/agents/" + agentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("REGISTERED"))
                .body("registrationToken", nullValue()) // consumed
                .body("certSerialNumber", notNullValue())
                .body("certExpiresAt", notNullValue())
                .body("certSubjectSan", equalTo(agentId + ".agents.qamelo.io"));
    }

    @Test
    void registerWithExpiredToken() {
        // Create agent
        var createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "expired-agent-" + UUID.randomUUID()))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract();

        String agentId = createResponse.path("id");
        String token = createResponse.path("registrationToken");

        // Manually expire the token by updating via repository
        agentRepository.findById(UUID.fromString(agentId))
                .chain(agent -> {
                    agent.setRegistrationTokenExpiresAt(Instant.now().minusSeconds(3600));
                    return agentRepository.update(agent);
                })
                .await().indefinitely();

        // Try to register with expired token -> 401
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("token", token, "csr", "FAKE_CSR"))
                .when().post("/api/v1/agents/register")
                .then()
                .statusCode(401)
                .body("error", equalTo("unauthorized"));
    }

    @Test
    void registerTokenConsumed() {
        // Create agent
        var createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "consumed-agent-" + UUID.randomUUID()))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract();

        String token = createResponse.path("registrationToken");

        // Register first time — success
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("token", token, "csr", "FAKE_CSR"))
                .when().post("/api/v1/agents/register")
                .then()
                .statusCode(200);

        // Try again with same token -> 401 (token was consumed/nulled)
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("token", token, "csr", "FAKE_CSR"))
                .when().post("/api/v1/agents/register")
                .then()
                .statusCode(401);
    }

    @Test
    void renewCert() {
        // Create + register
        var createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "renew-agent-" + UUID.randomUUID()))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract();

        String agentId = createResponse.path("id");
        String token = createResponse.path("registrationToken");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("token", token, "csr", "ORIGINAL_CSR"))
                .when().post("/api/v1/agents/register")
                .then()
                .statusCode(200);

        // Get original cert serial
        String originalSerial = given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/agents/" + agentId)
                .then()
                .statusCode(200)
                .extract().path("certSerialNumber");

        // Renew
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("csr", "NEW_CSR"))
                .when().post("/api/v1/agents/" + agentId + "/renew-cert")
                .then()
                .statusCode(200)
                .body("certificate", notNullValue())
                .body("caChain", notNullValue());

        // Verify serial changed
        String newSerial = given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/agents/" + agentId)
                .then()
                .statusCode(200)
                .extract().path("certSerialNumber");

        assertThat(newSerial).isNotEqualTo(originalSerial);
    }

    @Test
    void agentStatusReport() {
        // Create + register
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
                .body(Map.of("token", token, "csr", "STATUS_CSR"))
                .when().post("/api/v1/agents/register")
                .then()
                .statusCode(200);

        // Send status report
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("tunnelState", "CONNECTED", "remoteAddress", "10.0.0.42:9443"))
                .when().post("/api/v1/agents/" + agentId + "/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("CONNECTED"))
                .body("lastSeenAt", notNullValue())
                .body("tunnelRemoteAddress", equalTo("10.0.0.42:9443"));
    }

    @Test
    void agentConfig() {
        // Create + register
        var createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "config-agent-" + UUID.randomUUID()))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract();

        String agentId = createResponse.path("id");
        String token = createResponse.path("registrationToken");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("token", token, "csr", "CONFIG_CSR"))
                .when().post("/api/v1/agents/register")
                .then()
                .statusCode(200);

        // Get config
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/agents/" + agentId + "/config")
                .then()
                .statusCode(200)
                .body("id", equalTo(agentId))
                .body("status", equalTo("REGISTERED"));
    }
}
