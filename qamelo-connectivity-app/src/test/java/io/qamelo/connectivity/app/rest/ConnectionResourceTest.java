package io.qamelo.connectivity.app.rest;

import io.qamelo.connectivity.app.harness.TestSecretsClient;
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
class ConnectionResourceTest {

    @Inject
    TestSecretsClient testSecretsClient;

    @Test
    void createAndReadConnection() {
        // Create
        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "test-sftp-" + UUID.randomUUID(),
                        "type", "SFTP",
                        "host", "sftp.example.com",
                        "port", 22,
                        "authType", "BASIC",
                        "description", "Test SFTP connection"
                ))
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .body("name", startsWith("test-sftp-"))
                .body("type", equalTo("SFTP"))
                .body("host", equalTo("sftp.example.com"))
                .body("port", equalTo(22))
                .body("authType", equalTo("BASIC"))
                .body("hasCredentials", equalTo(false))
                .body("status", equalTo("ACTIVE"))
                .body("id", notNullValue())
                .body("createdAt", notNullValue())
                .extract().path("id");

        // Read back
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/connections/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("type", equalTo("SFTP"))
                .body("hasCredentials", equalTo(false));
    }

    @Test
    void listConnections() {
        // Create two connections
        for (int i = 0; i < 2; i++) {
            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer test-token")
                    .body(Map.of(
                            "name", "list-test-" + UUID.randomUUID(),
                            "type", "REST",
                            "host", "api.example.com",
                            "port", 443,
                            "authType", "API_KEY"
                    ))
                    .when().post("/api/v1/connections")
                    .then()
                    .statusCode(201);
        }

        // List all
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/connections")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2));
    }

    @Test
    void updateConnection() {
        // Create
        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "update-test-" + UUID.randomUUID(),
                        "type", "SFTP",
                        "host", "old.example.com",
                        "port", 22,
                        "authType", "BASIC"
                ))
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Update
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "updated-name-" + UUID.randomUUID(),
                        "type", "SFTP",
                        "host", "new.example.com",
                        "port", 2222,
                        "authType", "CERTIFICATE",
                        "status", "INACTIVE"
                ))
                .when().put("/api/v1/connections/" + id)
                .then()
                .statusCode(200)
                .body("host", equalTo("new.example.com"))
                .body("port", equalTo(2222))
                .body("authType", equalTo("CERTIFICATE"))
                .body("status", equalTo("INACTIVE"));
    }

    @Test
    void deleteConnection() {
        // Create
        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "delete-test-" + UUID.randomUUID(),
                        "type", "JDBC",
                        "host", "db.example.com",
                        "port", 5432,
                        "authType", "BASIC"
                ))
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Delete
        given()
                .header("Authorization", "Bearer test-token")
                .when().delete("/api/v1/connections/" + id)
                .then()
                .statusCode(204);

        // Verify gone
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/connections/" + id)
                .then()
                .statusCode(404);
    }

    @Test
    void getNotFound() {
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/connections/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void missingAuthReturns401() {
        given()
                .when().get("/api/v1/connections")
                .then()
                .statusCode(401);
    }

    @Test
    void forbiddenUserReturns403() {
        given()
                .header("Authorization", "Bearer forbidden-token")
                .when().get("/api/v1/connections")
                .then()
                .statusCode(403);
    }

    @Test
    void readOnlyUserCanList() {
        given()
                .header("Authorization", "Bearer reader-token")
                .when().get("/api/v1/connections")
                .then()
                .statusCode(200);
    }

    @Test
    void readOnlyUserCannotCreate() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer reader-token")
                .body(Map.of(
                        "name", "forbidden-create-" + UUID.randomUUID(),
                        "type", "REST",
                        "host", "api.example.com",
                        "port", 443,
                        "authType", "NONE"
                ))
                .when().post("/api/v1/connections")
                .then()
                .statusCode(403);
    }

    @Test
    void healthReadyReturnsUp() {
        given()
                .when().get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void healthLiveReturnsUp() {
        given()
                .when().get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void connectionWithProperties() {
        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "props-test-" + UUID.randomUUID(),
                        "type", "AS4",
                        "host", "as4.example.com",
                        "port", 443,
                        "authType", "CERTIFICATE",
                        "properties", Map.of("endpoint", "/msh", "security", "sign-and-encrypt")
                ))
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .body("properties.endpoint", equalTo("/msh"))
                .body("properties.security", equalTo("sign-and-encrypt"))
                .extract().path("id");

        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/connections/" + id)
                .then()
                .statusCode(200)
                .body("properties.endpoint", equalTo("/msh"));
    }

    // --- Phase 2: Credential integration tests ---

    @Test
    void createConnectionWithCredentials() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "cred-create-" + UUID.randomUUID());
        body.put("type", "SFTP");
        body.put("host", "sftp.example.com");
        body.put("port", 22);
        body.put("authType", "BASIC");
        body.put("credentials", Map.of("username", "admin", "password", "s3cret"));

        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(body)
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .body("hasCredentials", equalTo(true))
                .extract().path("id");

        // Verify credential was written to the test secrets store
        String vaultPath = "connectivity/connections/" + id + "/credentials";
        assertThat(testSecretsClient.getStore()).containsKey(vaultPath);
        assertThat(testSecretsClient.getStore().get(vaultPath))
                .containsEntry("username", "admin")
                .containsEntry("password", "s3cret");

        // Read back and confirm hasCredentials
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/connections/" + id)
                .then()
                .statusCode(200)
                .body("hasCredentials", equalTo(true));
    }

    @Test
    void updateConnectionWithCredentials() {
        // Create with credentials
        Map<String, Object> createBody = new HashMap<>();
        createBody.put("name", "cred-update-" + UUID.randomUUID());
        createBody.put("type", "SFTP");
        createBody.put("host", "sftp.example.com");
        createBody.put("port", 22);
        createBody.put("authType", "BASIC");
        createBody.put("credentials", Map.of("username", "admin", "password", "old-pass"));

        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(createBody)
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .body("hasCredentials", equalTo(true))
                .extract().path("id");

        // Update with new credentials
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", "cred-update-" + UUID.randomUUID());
        updateBody.put("type", "SFTP");
        updateBody.put("host", "sftp.example.com");
        updateBody.put("port", 22);
        updateBody.put("authType", "BASIC");
        updateBody.put("credentials", Map.of("username", "admin", "password", "new-pass"));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/connections/" + id)
                .then()
                .statusCode(200)
                .body("hasCredentials", equalTo(true));

        // Verify credential was overwritten in the test secrets store
        String vaultPath = "connectivity/connections/" + id + "/credentials";
        assertThat(testSecretsClient.getStore().get(vaultPath))
                .containsEntry("password", "new-pass");
    }

    @Test
    void updateConnectionWithoutCredentials() {
        // Create with credentials
        Map<String, Object> createBody = new HashMap<>();
        createBody.put("name", "cred-noop-" + UUID.randomUUID());
        createBody.put("type", "SFTP");
        createBody.put("host", "sftp.example.com");
        createBody.put("port", 22);
        createBody.put("authType", "BASIC");
        createBody.put("credentials", Map.of("username", "admin", "password", "keep-me"));

        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(createBody)
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .body("hasCredentials", equalTo(true))
                .extract().path("id");

        // Update without credentials field (null) — should leave unchanged
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "cred-noop-updated-" + UUID.randomUUID(),
                        "type", "SFTP",
                        "host", "sftp.example.com",
                        "port", 22,
                        "authType", "BASIC"
                ))
                .when().put("/api/v1/connections/" + id)
                .then()
                .statusCode(200)
                .body("hasCredentials", equalTo(true));

        // Verify credential untouched in the test secrets store
        String vaultPath = "connectivity/connections/" + id + "/credentials";
        assertThat(testSecretsClient.getStore().get(vaultPath))
                .containsEntry("password", "keep-me");
    }

    @Test
    void updateConnectionRemoveCredentials() {
        // Create with credentials
        Map<String, Object> createBody = new HashMap<>();
        createBody.put("name", "cred-remove-" + UUID.randomUUID());
        createBody.put("type", "SFTP");
        createBody.put("host", "sftp.example.com");
        createBody.put("port", 22);
        createBody.put("authType", "BASIC");
        createBody.put("credentials", Map.of("username", "admin", "password", "delete-me"));

        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(createBody)
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .body("hasCredentials", equalTo(true))
                .extract().path("id");

        // Update with empty credentials map — should delete from Vault
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", "cred-remove-" + UUID.randomUUID());
        updateBody.put("type", "SFTP");
        updateBody.put("host", "sftp.example.com");
        updateBody.put("port", 22);
        updateBody.put("authType", "BASIC");
        updateBody.put("credentials", Map.of());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/connections/" + id)
                .then()
                .statusCode(200)
                .body("hasCredentials", equalTo(false));

        // Verify credential removed from the test secrets store
        String vaultPath = "connectivity/connections/" + id + "/credentials";
        assertThat(testSecretsClient.getStore()).doesNotContainKey(vaultPath);
    }

    @Test
    void deleteConnectionWithChannels() {
        // Create connection
        String connectionId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "ch-block-conn-" + UUID.randomUUID(),
                        "type", "SFTP",
                        "host", "sftp.example.com",
                        "port", 22,
                        "authType", "BASIC"
                ))
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create partner (required for channel)
        String partnerId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "ch-block-partner-" + UUID.randomUUID()
                ))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create channel referencing the connection
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "ch-block-channel-" + UUID.randomUUID(),
                        "partnerId", partnerId,
                        "connectionId", connectionId,
                        "type", "SFTP",
                        "direction", "OUTBOUND"
                ))
                .when().post("/api/v1/channels")
                .then()
                .statusCode(201);

        // Try to delete connection -> 409 CONFLICT
        given()
                .header("Authorization", "Bearer test-token")
                .when().delete("/api/v1/connections/" + connectionId)
                .then()
                .statusCode(409)
                .body("error", equalTo("conflict"))
                .body("message", containsString("channels"));
    }

    @Test
    void deleteConnectionWithCredentials() {
        // Create with credentials
        Map<String, Object> createBody = new HashMap<>();
        createBody.put("name", "cred-delete-" + UUID.randomUUID());
        createBody.put("type", "SFTP");
        createBody.put("host", "sftp.example.com");
        createBody.put("port", 22);
        createBody.put("authType", "BASIC");
        createBody.put("credentials", Map.of("username", "admin", "password", "cleanup-me"));

        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(createBody)
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .body("hasCredentials", equalTo(true))
                .extract().path("id");

        String vaultPath = "connectivity/connections/" + id + "/credentials";
        assertThat(testSecretsClient.getStore()).containsKey(vaultPath);

        // Delete connection
        given()
                .header("Authorization", "Bearer test-token")
                .when().delete("/api/v1/connections/" + id)
                .then()
                .statusCode(204);

        // Verify credential cleaned up from Vault
        assertThat(testSecretsClient.getStore()).doesNotContainKey(vaultPath);

        // Verify connection gone
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/connections/" + id)
                .then()
                .statusCode(404);
    }
}
