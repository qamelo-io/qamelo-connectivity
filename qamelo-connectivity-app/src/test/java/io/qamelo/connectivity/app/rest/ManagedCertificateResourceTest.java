package io.qamelo.connectivity.app.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ManagedCertificateResourceTest {

    @Test
    void createPartnerCertificate() {
        // Create partner first
        String partnerId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "cert-partner-" + UUID.randomUUID()))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create certificate for the partner
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "test-tls-cert-" + UUID.randomUUID(),
                        "usage", "TLS_CLIENT",
                        "source", "VAULT_PKI_ISSUED",
                        "serialNumber", "AA:BB:CC:DD",
                        "subjectDn", "CN=test.example.com",
                        "issuerDn", "CN=Qamelo CA",
                        "notBefore", Instant.now().toString(),
                        "notAfter", Instant.now().plus(90, ChronoUnit.DAYS).toString()
                ))
                .when().post("/api/v1/partners/" + partnerId + "/certificates")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("partnerId", equalTo(partnerId))
                .body("status", equalTo("ACTIVE"))
                .body("usage", equalTo("TLS_CLIENT"))
                .body("source", equalTo("VAULT_PKI_ISSUED"))
                .body("serialNumber", equalTo("AA:BB:CC:DD"))
                .body("subjectDn", equalTo("CN=test.example.com"))
                .body("issuerDn", equalTo("CN=Qamelo CA"))
                .body("createdAt", notNullValue());
    }

    @Test
    void listPartnerCertificates() {
        // Create partner
        String partnerId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "list-cert-partner-" + UUID.randomUUID()))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create 2 certificates
        for (int i = 0; i < 2; i++) {
            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer test-token")
                    .body(Map.of(
                            "name", "list-cert-" + i + "-" + UUID.randomUUID(),
                            "usage", "SIGNING",
                            "source", "VAULT_KV_IMPORTED"
                    ))
                    .when().post("/api/v1/partners/" + partnerId + "/certificates")
                    .then()
                    .statusCode(201);
        }

        // List certificates
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/partners/" + partnerId + "/certificates")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2));
    }

    @Test
    void deletePartnerCertificate() {
        // Create partner
        String partnerId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "del-cert-partner-" + UUID.randomUUID()))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create certificate
        String certId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "delete-cert-" + UUID.randomUUID(),
                        "usage", "TLS_SERVER",
                        "source", "AGENT_LOCAL"
                ))
                .when().post("/api/v1/partners/" + partnerId + "/certificates")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Delete certificate
        given()
                .header("Authorization", "Bearer test-token")
                .when().delete("/api/v1/partners/" + partnerId + "/certificates/" + certId)
                .then()
                .statusCode(204);

        // Verify list is empty
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/partners/" + partnerId + "/certificates")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    void createCertificateWithInvalidUsageReturns400() {
        // Create partner
        String partnerId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "bad-usage-partner-" + UUID.randomUUID()))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Try to create cert with invalid usage
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "bad-cert-" + UUID.randomUUID(),
                        "usage", "INVALID_USAGE",
                        "source", "VAULT_PKI_ISSUED"
                ))
                .when().post("/api/v1/partners/" + partnerId + "/certificates")
                .then()
                .statusCode(400);
    }

    @Test
    void listCertsForNonExistentPartnerReturns404() {
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/partners/" + UUID.randomUUID() + "/certificates")
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }
}
