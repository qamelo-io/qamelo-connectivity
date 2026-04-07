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
class RoutingTableTest {

    @Test
    void routingTableEndpointReturnsEntriesArray() {
        // Verify the routing table endpoint returns a valid response with an entries array.
        // Other tests may have created agents/VHs, so we just verify the shape.
        given()
                .when().get("/api/v1/internal/routing-table")
                .then()
                .statusCode(200)
                .body("entries", notNullValue());
    }

    @Test
    void routingTableIncludesRegisteredAgentWithActiveVirtualHost() {
        // Create agent
        var createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "rt-agent-" + UUID.randomUUID()))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract();

        String agentId = createResponse.path("id");
        String token = createResponse.path("registrationToken");

        // Register agent (PENDING -> REGISTERED)
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("token", token, "csr", "FAKE_CSR"))
                .when().post("/api/v1/agents/register")
                .then()
                .statusCode(200);

        // Add virtual host
        String hostname = "rt-vh-" + UUID.randomUUID();
        Map<String, Object> vhBody = new HashMap<>();
        vhBody.put("hostname", hostname);
        vhBody.put("targetHost", "10.0.0.50");
        vhBody.put("targetPort", 8080);
        vhBody.put("protocol", "HTTP");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(vhBody)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201);

        // Get routing table — should include the entry
        given()
                .when().get("/api/v1/internal/routing-table")
                .then()
                .statusCode(200)
                .body("entries.size()", greaterThanOrEqualTo(1))
                .body("entries.find { it.agentId == '" + agentId + "' }.virtualHostname", equalTo(hostname))
                .body("entries.find { it.agentId == '" + agentId + "' }.targetHost", equalTo("10.0.0.50"))
                .body("entries.find { it.agentId == '" + agentId + "' }.targetPort", equalTo(8080))
                .body("entries.find { it.agentId == '" + agentId + "' }.protocol", equalTo("HTTP"));
    }

    @Test
    void routingTableOnlyActiveVirtualHosts() {
        // Create + register agent
        var createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "rt-filter-agent-" + UUID.randomUUID()))
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

        // Add ACTIVE virtual host
        String activeHostname = "rt-active-" + UUID.randomUUID();
        Map<String, Object> activeBody = new HashMap<>();
        activeBody.put("hostname", activeHostname);
        activeBody.put("targetHost", "10.0.0.51");
        activeBody.put("targetPort", 8081);
        activeBody.put("status", "ACTIVE");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(activeBody)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201);

        // Add INACTIVE virtual host
        String inactiveHostname = "rt-inactive-" + UUID.randomUUID();
        Map<String, Object> inactiveBody = new HashMap<>();
        inactiveBody.put("hostname", inactiveHostname);
        inactiveBody.put("targetHost", "10.0.0.52");
        inactiveBody.put("targetPort", 8082);
        inactiveBody.put("status", "INACTIVE");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(inactiveBody)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201);

        // Get routing table — only ACTIVE should appear
        given()
                .when().get("/api/v1/internal/routing-table")
                .then()
                .statusCode(200)
                .body("entries.virtualHostname", hasItem(activeHostname))
                .body("entries.virtualHostname", not(hasItem(inactiveHostname)));
    }

    @Test
    void routingTableExcludesPendingAgents() {
        // Create agent but don't register (stays PENDING)
        String agentId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "rt-pending-agent-" + UUID.randomUUID()))
                .when().post("/api/v1/agents")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Add virtual host to the PENDING agent
        String hostname = "rt-pending-vh-" + UUID.randomUUID();
        Map<String, Object> vhBody = new HashMap<>();
        vhBody.put("hostname", hostname);
        vhBody.put("targetHost", "10.0.0.60");
        vhBody.put("targetPort", 9090);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(vhBody)
                .when().post("/api/v1/agents/" + agentId + "/virtual-hosts")
                .then()
                .statusCode(201);

        // Routing table should NOT include VHs from PENDING agents
        given()
                .when().get("/api/v1/internal/routing-table")
                .then()
                .statusCode(200)
                .body("entries.virtualHostname", not(hasItem(hostname)));
    }
}
