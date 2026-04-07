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
class AgreementResourceTest {

    private String createPartner() {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "agr-partner-" + UUID.randomUUID()
                ))
                .when().post("/api/v1/partners")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private String createConnection() {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "agr-conn-" + UUID.randomUUID(),
                        "type", "AS4",
                        "host", "as4.example.com",
                        "port", 443,
                        "authType", "CERTIFICATE"
                ))
                .when().post("/api/v1/connections")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private String createChannel(String partnerId, String connectionId) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "agr-channel-" + UUID.randomUUID(),
                        "partnerId", partnerId,
                        "connectionId", connectionId,
                        "type", "AS4",
                        "direction", "OUTBOUND"
                ))
                .when().post("/api/v1/channels")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private String createAgreement(String channelId) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "agreement-" + UUID.randomUUID(),
                        "channelId", channelId,
                        "documentType", "INVOICE",
                        "direction", "SEND"
                ))
                .when().post("/api/v1/agreements")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    void createAgreement() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "test-agreement-" + UUID.randomUUID(),
                        "channelId", channelId,
                        "documentType", "INVOICE",
                        "direction", "SEND"
                ))
                .when().post("/api/v1/agreements")
                .then()
                .statusCode(201)
                .body("name", startsWith("test-agreement-"))
                .body("channelId", equalTo(channelId))
                .body("documentType", equalTo("INVOICE"))
                .body("direction", equalTo("SEND"))
                .body("status", equalTo("DRAFT"))
                .body("version", equalTo(0))
                .body("id", notNullValue())
                .body("createdAt", notNullValue());
    }

    @Test
    void getAgreement() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);
        String agreementId = createAgreement(channelId);

        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(200)
                .body("id", equalTo(agreementId))
                .body("channelId", equalTo(channelId))
                .body("status", equalTo("DRAFT"));
    }

    @Test
    void listAgreements() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);
        createAgreement(channelId);
        createAgreement(channelId);

        given()
                .header("Authorization", "Bearer test-token")
                .queryParam("channelId", channelId)
                .when().get("/api/v1/agreements")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2));
    }

    @Test
    void agreementStatusLifecycle() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);

        String name = "lifecycle-" + UUID.randomUUID();
        String agreementId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", name,
                        "channelId", channelId,
                        "documentType", "ORDER",
                        "direction", "SEND"
                ))
                .when().post("/api/v1/agreements")
                .then()
                .statusCode(201)
                .body("status", equalTo("DRAFT"))
                .extract().path("id");

        // DRAFT -> ACTIVE
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", name);
        updateBody.put("channelId", channelId);
        updateBody.put("documentType", "ORDER");
        updateBody.put("direction", "SEND");
        updateBody.put("status", "ACTIVE");
        updateBody.put("version", 0);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"))
                .body("version", equalTo(1));

        // ACTIVE -> SUSPENDED
        updateBody.put("status", "SUSPENDED");
        updateBody.put("version", 1);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(200)
                .body("status", equalTo("SUSPENDED"))
                .body("version", equalTo(2));

        // SUSPENDED -> ACTIVE
        updateBody.put("status", "ACTIVE");
        updateBody.put("version", 2);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"))
                .body("version", equalTo(3));

        // ACTIVE -> TERMINATED
        updateBody.put("status", "TERMINATED");
        updateBody.put("version", 3);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(200)
                .body("status", equalTo("TERMINATED"))
                .body("version", equalTo(4));
    }

    @Test
    void invalidStatusTransition() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);

        String name = "invalid-transition-" + UUID.randomUUID();
        String agreementId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", name,
                        "channelId", channelId,
                        "documentType", "ORDER",
                        "direction", "RECEIVE"
                ))
                .when().post("/api/v1/agreements")
                .then()
                .statusCode(201)
                .body("status", equalTo("DRAFT"))
                .extract().path("id");

        // DRAFT -> SUSPENDED (invalid)
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", name);
        updateBody.put("channelId", channelId);
        updateBody.put("documentType", "ORDER");
        updateBody.put("direction", "RECEIVE");
        updateBody.put("status", "SUSPENDED");
        updateBody.put("version", 0);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(400)
                .body("error", equalTo("bad_request"))
                .body("message", containsString("Invalid status transition"));
    }

    @Test
    void terminalStatusCannotChange() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);

        String name = "terminal-" + UUID.randomUUID();
        String agreementId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", name,
                        "channelId", channelId,
                        "documentType", "ORDER",
                        "direction", "SEND"
                ))
                .when().post("/api/v1/agreements")
                .then()
                .statusCode(201)
                .extract().path("id");

        // DRAFT -> ACTIVE
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", name);
        updateBody.put("channelId", channelId);
        updateBody.put("documentType", "ORDER");
        updateBody.put("direction", "SEND");
        updateBody.put("status", "ACTIVE");
        updateBody.put("version", 0);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(200);

        // ACTIVE -> TERMINATED
        updateBody.put("status", "TERMINATED");
        updateBody.put("version", 1);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(200);

        // TERMINATED -> ACTIVE (invalid)
        updateBody.put("status", "ACTIVE");
        updateBody.put("version", 2);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(400)
                .body("error", equalTo("bad_request"))
                .body("message", containsString("Invalid status transition"));
    }

    @Test
    void optimisticLocking() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);

        String name = "opt-lock-" + UUID.randomUUID();
        String agreementId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", name,
                        "channelId", channelId,
                        "documentType", "INVOICE",
                        "direction", "SEND"
                ))
                .when().post("/api/v1/agreements")
                .then()
                .statusCode(201)
                .body("version", equalTo(0))
                .extract().path("id");

        // First update with version=0 -> success
        String updatedName = "opt-lock-updated-" + UUID.randomUUID();
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", updatedName);
        updateBody.put("channelId", channelId);
        updateBody.put("documentType", "INVOICE");
        updateBody.put("direction", "SEND");
        updateBody.put("version", 0);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(200)
                .body("version", equalTo(1));

        // Second update with stale version=0 -> 409
        updateBody.put("name", "opt-lock-stale-" + UUID.randomUUID());
        updateBody.put("version", 0);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(409)
                .body("error", equalTo("conflict"));
    }

    @Test
    void agreementWithPmodeProperties() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "pmode-agreement-" + UUID.randomUUID());
        body.put("channelId", channelId);
        body.put("documentType", "AS4_INVOICE");
        body.put("direction", "SEND");
        body.put("pmodeProperties", Map.of(
                "action", "DeliverInvoice",
                "service", "http://docs.oasis-open.org/ebxml-msg/as4/200902/service",
                "partyType", "urn:oasis:names:tc:ebcore:partyid-type:iso6523:0088"
        ));

        String agreementId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(body)
                .when().post("/api/v1/agreements")
                .then()
                .statusCode(201)
                .body("pmodeProperties.action", equalTo("DeliverInvoice"))
                .body("pmodeProperties.service", equalTo("http://docs.oasis-open.org/ebxml-msg/as4/200902/service"))
                .extract().path("id");

        // Read back
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(200)
                .body("pmodeProperties.action", equalTo("DeliverInvoice"));
    }

    @Test
    void agreementWithRetryPolicy() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "retry-agreement-" + UUID.randomUUID());
        body.put("channelId", channelId);
        body.put("documentType", "ORDER");
        body.put("direction", "SEND");
        body.put("retryPolicy", Map.of(
                "maxRetries", 5,
                "backoffSeconds", 30,
                "backoffMultiplier", 2.0
        ));
        body.put("slaDeadlineMinutes", 60);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(body)
                .when().post("/api/v1/agreements")
                .then()
                .statusCode(201)
                .body("retryPolicy.maxRetries", equalTo(5))
                .body("retryPolicy.backoffSeconds", equalTo(30))
                .body("retryPolicy.backoffMultiplier", equalTo(2.0f))
                .body("slaDeadlineMinutes", equalTo(60));
    }

    @Test
    void deleteAgreementDraftAllowed() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);
        String agreementId = createAgreement(channelId);

        // Delete DRAFT -> 204
        given()
                .header("Authorization", "Bearer test-token")
                .when().delete("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(204);

        // Verify gone
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(404);
    }

    @Test
    void deleteAgreementActiveRejected() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);

        String name = "delete-active-" + UUID.randomUUID();
        String agreementId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", name,
                        "channelId", channelId,
                        "documentType", "INVOICE",
                        "direction", "SEND"
                ))
                .when().post("/api/v1/agreements")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Activate it
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", name);
        updateBody.put("channelId", channelId);
        updateBody.put("documentType", "INVOICE");
        updateBody.put("direction", "SEND");
        updateBody.put("status", "ACTIVE");
        updateBody.put("version", 0);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(200);

        // Try to delete ACTIVE -> 409
        given()
                .header("Authorization", "Bearer test-token")
                .when().delete("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(409)
                .body("error", equalTo("conflict"))
                .body("message", containsString("DRAFT or TERMINATED"));
    }

    @Test
    void softRefsAcceptNull() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "null-refs-" + UUID.randomUUID());
        body.put("channelId", channelId);
        body.put("documentType", "ORDER");
        body.put("direction", "RECEIVE");
        body.put("packageId", null);
        body.put("artifactId", null);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(body)
                .when().post("/api/v1/agreements")
                .then()
                .statusCode(201)
                .body("packageId", nullValue())
                .body("artifactId", nullValue());
    }
}
