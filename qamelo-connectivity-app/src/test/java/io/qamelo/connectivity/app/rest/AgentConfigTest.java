package io.qamelo.connectivity.app.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AgentConfigTest {

    @Test
    void configIncludesCredentialRefsFromLinkedConnection() {
        // Create connection WITH credentials (so vaultCredentialPath is set)
        Map<String, Object> connBody = new HashMap<>();
        connBody.put("name", "cfg-conn-" + UUID.randomUUID());
        connBody.put("type", "REST");
        connBody.put("host", "api.example.com");
        connBody.put("port", 443);
        connBody.put("authType", "API_KEY");
        connBody.put("credentials", Map.of("apiKey", "secret-key-123"));

        String connectionId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(connBody)
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .body("hasCredentials", equalTo(true))
                .extract().path("id");

        // Create + register agent
        var createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "cfg-agent-" + UUID.randomUUID()))
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

        // Add VH linked to the connection
        Map<String, Object> vhBody = new HashMap<>();
        vhBody.put("hostname", "cfg-vh-" + UUID.randomUUID());
        vhBody.put("targetHost", "10.0.0.80");
        vhBody.put("targetPort", 443);
        vhBody.put("connectionId", connectionId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(vhBody)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201);

        // GET config — should include agent, virtualHosts with vaultCredentialPath, gatewayUrl, configVersion
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/agents/" + agentId + "/config")
                .then()
                .statusCode(200)
                .body("agent.id", equalTo(agentId))
                .body("virtualHosts.size()", equalTo(1))
                .body("virtualHosts[0].virtualHost.connectionId", equalTo(connectionId))
                .body("virtualHosts[0].vaultCredentialPath", notNullValue())
                .body("virtualHosts[0].vaultCredentialPath", containsString("connectivity/connections/"))
                .body("gatewayUrl", equalTo("https://gateway.qamelo.io"))
                .body("configVersion", notNullValue());
    }

    @Test
    void configWithNoLinkedConnections() {
        // Create + register agent
        var createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "cfg-noconn-agent-" + UUID.randomUUID()))
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

        // Add VH WITHOUT connectionId
        Map<String, Object> vhBody = new HashMap<>();
        vhBody.put("hostname", "cfg-noconn-vh-" + UUID.randomUUID());
        vhBody.put("targetHost", "10.0.0.81");
        vhBody.put("targetPort", 8080);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(vhBody)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201);

        // GET config — vaultCredentialPath should be null
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/agents/" + agentId + "/config")
                .then()
                .statusCode(200)
                .body("agent.id", equalTo(agentId))
                .body("virtualHosts.size()", equalTo(1))
                .body("virtualHosts[0].vaultCredentialPath", nullValue())
                .body("gatewayUrl", equalTo("https://gateway.qamelo.io"))
                .body("configVersion", notNullValue());
    }
}
