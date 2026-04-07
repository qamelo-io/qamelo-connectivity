package io.qamelo.connectivity.app.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class InternalAgreementResourceTest {

    private String createPartner(String name, String identifierScheme, String identifierValue) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        if (identifierScheme != null) {
            body.put("identifiers", List.of(Map.of(
                    "scheme", identifierScheme,
                    "value", identifierValue
            )));
        }
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(body)
                .when().post("/api/v1/partners")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

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

    private String createChannel(String name, String partnerId, String connectionId, String type, String direction) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", name,
                        "partnerId", partnerId,
                        "connectionId", connectionId,
                        "type", type,
                        "direction", direction
                ))
                .when().post("/api/v1/channels")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private String createAgreement(String name, String channelId, Map<String, String> pmodeProperties) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("channelId", channelId);
        body.put("documentType", "INVOICE");
        body.put("direction", "SEND");
        if (pmodeProperties != null) {
            body.put("pmodeProperties", pmodeProperties);
        }
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(body)
                .when().post("/api/v1/agreements")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private String activateAgreement(String agreementId, String name, String channelId) {
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", name);
        updateBody.put("channelId", channelId);
        updateBody.put("documentType", "INVOICE");
        updateBody.put("direction", "SEND");
        updateBody.put("status", "ACTIVE");
        updateBody.put("version", 0);

        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(updateBody)
                .when().put("/api/v1/agreements/" + agreementId)
                .then()
                .statusCode(200)
                .extract().path("id");
    }

    @Test
    void matchAgreement() {
        String partnerId = createPartner("match-partner-" + UUID.randomUUID(), "DUNS", "123456789");
        String connectionId = createConnection("match-conn-" + UUID.randomUUID());
        String channelId = createChannel("match-channel-" + UUID.randomUUID(), partnerId, connectionId, "AS4", "INBOUND");

        Map<String, String> pmode = Map.of("service", "urn:test:service", "action", "deliver");
        String agreementName = "match-agreement-" + UUID.randomUUID();
        createAgreement(agreementName, channelId, pmode);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "channelType", "AS4",
                        "criteria", Map.of("service", "urn:test:service", "action", "deliver")
                ))
                .when().post("/api/v1/internal/agreements/match")
                .then()
                .statusCode(200)
                .body("agreementId", notNullValue())
                .body("agreementName", equalTo(agreementName))
                .body("documentType", equalTo("INVOICE"))
                .body("pmodeProperties.service", equalTo("urn:test:service"))
                .body("pmodeProperties.action", equalTo("deliver"))
                .body("channelId", equalTo(channelId))
                .body("channelType", equalTo("AS4"))
                .body("channelDirection", equalTo("INBOUND"))
                .body("connectionId", equalTo(connectionId))
                .body("connectionName", startsWith("match-conn-"))
                .body("connectionHost", equalTo("sftp.example.com"))
                .body("connectionPort", equalTo(22))
                .body("connectionAuthType", equalTo("BASIC"))
                .body("partnerId", equalTo(partnerId))
                .body("partnerName", startsWith("match-partner-"))
                .body("partnerIdentifiers.size()", equalTo(1))
                .body("partnerIdentifiers[0].scheme", equalTo("DUNS"))
                .body("partnerIdentifiers[0].value", equalTo("123456789"));
    }

    @Test
    void matchNoResult() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "channelType", "AS4",
                        "criteria", Map.of("service", "urn:nonexistent:service-" + UUID.randomUUID())
                ))
                .when().post("/api/v1/internal/agreements/match")
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void matchAmbiguous() {
        String partnerId = createPartner("ambig-partner-" + UUID.randomUUID(), "DUNS", "987654321");
        String connectionId = createConnection("ambig-conn-" + UUID.randomUUID());
        String channelId = createChannel("ambig-channel-" + UUID.randomUUID(), partnerId, connectionId, "AS4", "INBOUND");

        Map<String, String> pmode = Map.of("service", "urn:ambiguous:service", "action", "process");
        createAgreement("ambig-agr-1-" + UUID.randomUUID(), channelId, pmode);
        createAgreement("ambig-agr-2-" + UUID.randomUUID(), channelId, pmode);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "channelType", "AS4",
                        "criteria", Map.of("service", "urn:ambiguous:service", "action", "process")
                ))
                .when().post("/api/v1/internal/agreements/match")
                .then()
                .statusCode(409)
                .body("error", equalTo("conflict"))
                .body("message", containsString("Ambiguous"));
    }

    @Test
    void getAgreementById() {
        String partnerId = createPartner("get-partner-" + UUID.randomUUID(), "DUNS", "111222333");
        String connectionId = createConnection("get-conn-" + UUID.randomUUID());
        String channelId = createChannel("get-channel-" + UUID.randomUUID(), partnerId, connectionId, "SFTP", "OUTBOUND");
        String agreementId = createAgreement("get-agreement-" + UUID.randomUUID(), channelId, null);

        given()
                .when().get("/api/v1/internal/agreements/" + agreementId)
                .then()
                .statusCode(200)
                .body("agreementId", equalTo(agreementId))
                .body("channelId", equalTo(channelId))
                .body("connectionId", equalTo(connectionId))
                .body("partnerId", equalTo(partnerId))
                .body("connectionType", equalTo("SFTP"))
                .body("channelType", equalTo("SFTP"))
                .body("channelDirection", equalTo("OUTBOUND"));
    }

    @Test
    void getAgreementNotFound() {
        given()
                .when().get("/api/v1/internal/agreements/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }
}
