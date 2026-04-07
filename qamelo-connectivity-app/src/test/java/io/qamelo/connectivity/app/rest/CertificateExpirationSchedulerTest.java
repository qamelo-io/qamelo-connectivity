package io.qamelo.connectivity.app.rest;

import io.qamelo.connectivity.app.reconciler.CertificateExpirationScheduler;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class CertificateExpirationSchedulerTest {

    @Inject
    CertificateExpirationScheduler scheduler;

    @Test
    void expiringCertTransitionsToExpiringSoon() {
        // Create partner
        String partnerId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "expiring-partner-" + UUID.randomUUID()))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create cert that expires in 20 days (within 30-day threshold)
        String certId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "expiring-cert-" + UUID.randomUUID(),
                        "usage", "TLS_CLIENT",
                        "source", "VAULT_PKI_ISSUED",
                        "notAfter", Instant.now().plus(20, ChronoUnit.DAYS).toString()
                ))
                .when().post("/api/v1/partners/" + partnerId + "/certificates")
                .then()
                .statusCode(201)
                .body("status", equalTo("ACTIVE"))
                .extract().path("id");

        // Run scheduler
        scheduler.checkExpirations().await().indefinitely();

        // Verify status changed to EXPIRING_SOON
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/partners/" + partnerId + "/certificates")
                .then()
                .statusCode(200)
                .body("find { it.id == '" + certId + "' }.status", equalTo("EXPIRING_SOON"));
    }

    @Test
    void expiredCertTransitionsToExpired() {
        // Create partner
        String partnerId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "expired-partner-" + UUID.randomUUID()))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create cert that already expired (1 day ago)
        String certId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "expired-cert-" + UUID.randomUUID(),
                        "usage", "SIGNING",
                        "source", "VAULT_KV_IMPORTED",
                        "notAfter", Instant.now().minus(1, ChronoUnit.DAYS).toString()
                ))
                .when().post("/api/v1/partners/" + partnerId + "/certificates")
                .then()
                .statusCode(201)
                .body("status", equalTo("ACTIVE"))
                .extract().path("id");

        // Run scheduler
        scheduler.checkExpirations().await().indefinitely();

        // Verify status changed to EXPIRED
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/partners/" + partnerId + "/certificates")
                .then()
                .statusCode(200)
                .body("find { it.id == '" + certId + "' }.status", equalTo("EXPIRED"));
    }

    @Test
    void activeCertNotExpiringSoonUnchanged() {
        // Create partner
        String partnerId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of("name", "active-partner-" + UUID.randomUUID()))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create cert that expires in 60 days (outside 30-day threshold)
        String certId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "active-cert-" + UUID.randomUUID(),
                        "usage", "ENCRYPTION",
                        "source", "AGENT_LOCAL",
                        "notAfter", Instant.now().plus(60, ChronoUnit.DAYS).toString()
                ))
                .when().post("/api/v1/partners/" + partnerId + "/certificates")
                .then()
                .statusCode(201)
                .body("status", equalTo("ACTIVE"))
                .extract().path("id");

        // Run scheduler
        scheduler.checkExpirations().await().indefinitely();

        // Verify status still ACTIVE
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/partners/" + partnerId + "/certificates")
                .then()
                .statusCode(200)
                .body("find { it.id == '" + certId + "' }.status", equalTo("ACTIVE"));
    }
}
