package io.qamelo.connectivity.app.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ConnectionResourceTest {

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
}
