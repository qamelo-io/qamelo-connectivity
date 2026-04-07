package io.qamelo.connectivity.app.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class InternalConnectionResourceTest {

    private String createConnection(String name) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", name,
                        "type", "SFTP",
                        "host", "sftp.example.com",
                        "port", 22,
                        "authType", "BASIC"
                ))
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    void resolveKnownConnections() {
        String name1 = "resolve-known-1-" + UUID.randomUUID();
        String name2 = "resolve-known-2-" + UUID.randomUUID();
        createConnection(name1);
        createConnection(name2);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("names", List.of(name1, name2)))
                .when().post("/api/v1/internal/connections/resolve")
                .then()
                .statusCode(200)
                .body("resolved.size()", equalTo(2))
                .body("resolved.name", hasItems(name1, name2))
                .body("resolved[0].id", notNullValue())
                .body("resolved[0].type", equalTo("SFTP"))
                .body("resolved[0].host", equalTo("sftp.example.com"))
                .body("resolved[0].port", equalTo(22))
                .body("resolved[0].authType", equalTo("BASIC"))
                .body("resolved[0].status", equalTo("ACTIVE"))
                .body("notFound.size()", equalTo(0));
    }

    @Test
    void resolveUnknownConnections() {
        String unknown1 = "unknown-conn-" + UUID.randomUUID();
        String unknown2 = "unknown-conn-" + UUID.randomUUID();

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("names", List.of(unknown1, unknown2)))
                .when().post("/api/v1/internal/connections/resolve")
                .then()
                .statusCode(200)
                .body("resolved.size()", equalTo(0))
                .body("notFound.size()", equalTo(2))
                .body("notFound", hasItems(unknown1, unknown2));
    }

    @Test
    void resolveMixedKnownUnknown() {
        String knownName = "resolve-mixed-known-" + UUID.randomUUID();
        String unknownName = "resolve-mixed-unknown-" + UUID.randomUUID();
        createConnection(knownName);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("names", List.of(knownName, unknownName)))
                .when().post("/api/v1/internal/connections/resolve")
                .then()
                .statusCode(200)
                .body("resolved.size()", equalTo(1))
                .body("resolved[0].name", equalTo(knownName))
                .body("notFound.size()", equalTo(1))
                .body("notFound[0]", equalTo(unknownName));
    }

    @Test
    void resolveEmptyList() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("names", List.of()))
                .when().post("/api/v1/internal/connections/resolve")
                .then()
                .statusCode(200)
                .body("resolved.size()", equalTo(0))
                .body("notFound.size()", equalTo(0));
    }
}
