package io.qamelo.connectivity.app.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class PartnerResourceTest {

    @Test
    void createPartnerWithIdentifiers() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "partner-duns-gln-" + UUID.randomUUID(),
                        "description", "Test partner with DUNS and GLN",
                        "identifiers", List.of(
                                Map.of("scheme", "DUNS", "value", "123456789"),
                                Map.of("scheme", "GLN", "value", "1234567890123")
                        )
                ))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(201)
                .body("name", startsWith("partner-duns-gln-"))
                .body("description", equalTo("Test partner with DUNS and GLN"))
                .body("id", notNullValue())
                .body("createdAt", notNullValue())
                .body("identifiers.size()", equalTo(2))
                .body("identifiers.scheme", hasItems("DUNS", "GLN"))
                .body("identifiers.value", hasItems("123456789", "1234567890123"));
    }

    @Test
    void getPartner() {
        // Create
        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "get-partner-" + UUID.randomUUID(),
                        "identifiers", List.of(
                                Map.of("scheme", "VAT", "value", "CZ12345678")
                        )
                ))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Read back
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/partners/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("identifiers.size()", equalTo(1))
                .body("identifiers[0].scheme", equalTo("VAT"))
                .body("identifiers[0].value", equalTo("CZ12345678"));
    }

    @Test
    void updatePartnerIdentifiers() {
        // Create with DUNS
        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "update-partner-" + UUID.randomUUID(),
                        "identifiers", List.of(
                                Map.of("scheme", "DUNS", "value", "111111111")
                        )
                ))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(201)
                .body("identifiers.size()", equalTo(1))
                .extract().path("id");

        // Update — replace DUNS with GLN and ICO
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "updated-partner-" + UUID.randomUUID(),
                        "identifiers", List.of(
                                Map.of("scheme", "GLN", "value", "9999999999999"),
                                Map.of("scheme", "ICO", "value", "12345678")
                        )
                ))
                .when().put("/api/v1/partners/" + id)
                .then()
                .statusCode(200)
                .body("identifiers.size()", equalTo(2))
                .body("identifiers.scheme", hasItems("GLN", "ICO"))
                .body("identifiers.scheme", not(hasItem("DUNS")));
    }

    @Test
    void deletePartner() {
        // Create
        String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "delete-partner-" + UUID.randomUUID()
                ))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Delete
        given()
                .header("Authorization", "Bearer test-token")
                .when().delete("/api/v1/partners/" + id)
                .then()
                .statusCode(204);

        // Verify gone
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/partners/" + id)
                .then()
                .statusCode(404);
    }

    @Disabled("TODO Phase 4: channels table does not exist yet — hasChannels() always returns false")
    @Test
    void deletePartnerWithChannels() {
        // When channels exist, deleting should return 409 CONFLICT
    }

    @Test
    void invalidDunsReturns400() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "bad-duns-" + UUID.randomUUID(),
                        "identifiers", List.of(
                                Map.of("scheme", "DUNS", "value", "12345") // too short
                        )
                ))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(400)
                .body("message", containsString("DUNS must be exactly 9 digits"));
    }

    @Test
    void invalidGlnReturns400() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "bad-gln-" + UUID.randomUUID(),
                        "identifiers", List.of(
                                Map.of("scheme", "GLN", "value", "123") // too short
                        )
                ))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(400)
                .body("message", containsString("GLN must be exactly 13 digits"));
    }

    @Test
    void customSchemeRequiresLabel() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "bad-custom-" + UUID.randomUUID(),
                        "identifiers", List.of(
                                Map.of("scheme", "CUSTOM", "value", "ABC123")
                                // missing customSchemeLabel
                        )
                ))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(400)
                .body("message", containsString("customSchemeLabel"));
    }

    @Test
    void findByIdentifier() {
        String uniqueValue = "999888777";
        String name = "find-by-id-" + UUID.randomUUID();

        // Create partner with DUNS
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", name,
                        "identifiers", List.of(
                                Map.of("scheme", "DUNS", "value", uniqueValue)
                        )
                ))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(201);

        // Find by identifier
        given()
                .header("Authorization", "Bearer test-token")
                .queryParam("scheme", "DUNS")
                .queryParam("value", uniqueValue)
                .when().get("/api/v1/partners/by-identifier")
                .then()
                .statusCode(200)
                .body("name", equalTo(name))
                .body("identifiers.size()", greaterThanOrEqualTo(1));
    }

    @Test
    void partnerNotFound() {
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/partners/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }
}
