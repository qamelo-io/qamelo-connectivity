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
class ChannelResourceTest {

    private String createPartner() {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "ch-partner-" + UUID.randomUUID()
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
                        "name", "ch-conn-" + UUID.randomUUID(),
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

    private String createChannel(String partnerId, String connectionId) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "channel-" + UUID.randomUUID(),
                        "partnerId", partnerId,
                        "connectionId", connectionId,
                        "type", "SFTP",
                        "direction", "OUTBOUND"
                ))
                .when().post("/api/v1/channels")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    void createChannel() {
        String partnerId = createPartner();
        String connectionId = createConnection();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "test-channel-" + UUID.randomUUID(),
                        "partnerId", partnerId,
                        "connectionId", connectionId,
                        "type", "AS4",
                        "direction", "OUTBOUND"
                ))
                .when().post("/api/v1/channels")
                .then()
                .statusCode(201)
                .body("name", startsWith("test-channel-"))
                .body("type", equalTo("AS4"))
                .body("direction", equalTo("OUTBOUND"))
                .body("partnerId", equalTo(partnerId))
                .body("connectionId", equalTo(connectionId))
                .body("enabled", equalTo(true))
                .body("id", notNullValue())
                .body("createdAt", notNullValue());
    }

    @Test
    void getChannel() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);

        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/channels/" + channelId)
                .then()
                .statusCode(200)
                .body("id", equalTo(channelId))
                .body("partnerId", equalTo(partnerId))
                .body("connectionId", equalTo(connectionId))
                .body("type", equalTo("SFTP"))
                .body("direction", equalTo("OUTBOUND"));
    }

    @Test
    void listChannels() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        createChannel(partnerId, connectionId);
        createChannel(partnerId, connectionId);

        // List filtered by partnerId
        given()
                .header("Authorization", "Bearer test-token")
                .queryParam("partnerId", partnerId)
                .when().get("/api/v1/channels")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2));
    }

    @Test
    void updateChannel() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "updated-channel-" + UUID.randomUUID(),
                        "partnerId", partnerId,
                        "connectionId", connectionId,
                        "type", "AS2",
                        "direction", "INBOUND",
                        "enabled", false
                ))
                .when().put("/api/v1/channels/" + channelId)
                .then()
                .statusCode(200)
                .body("type", equalTo("AS2"))
                .body("direction", equalTo("INBOUND"))
                .body("enabled", equalTo(false));
    }

    @Test
    void deleteChannel() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);

        given()
                .header("Authorization", "Bearer test-token")
                .when().delete("/api/v1/channels/" + channelId)
                .then()
                .statusCode(204);

        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/v1/channels/" + channelId)
                .then()
                .statusCode(404);
    }

    @Test
    void deleteChannelWithAgreements() {
        String partnerId = createPartner();
        String connectionId = createConnection();
        String channelId = createChannel(partnerId, connectionId);

        // Create an agreement referencing this channel
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "agreement-block-delete-" + UUID.randomUUID(),
                        "channelId", channelId,
                        "documentType", "INVOICE",
                        "direction", "SEND"
                ))
                .when().post("/api/v1/agreements")
                .then()
                .statusCode(201);

        // Try to delete channel -> 409
        given()
                .header("Authorization", "Bearer test-token")
                .when().delete("/api/v1/channels/" + channelId)
                .then()
                .statusCode(409)
                .body("error", equalTo("conflict"))
                .body("message", containsString("agreements"));
    }

    @Test
    void invalidPartnerIdReturns400() {
        String connectionId = createConnection();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "bad-partner-channel-" + UUID.randomUUID(),
                        "partnerId", UUID.randomUUID().toString(),
                        "connectionId", connectionId,
                        "type", "SFTP",
                        "direction", "OUTBOUND"
                ))
                .when().post("/api/v1/channels")
                .then()
                .statusCode(400)
                .body("error", equalTo("bad_request"))
                .body("message", containsString("Partner not found"));
    }

    @Test
    void invalidConnectionIdReturns400() {
        String partnerId = createPartner();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "bad-conn-channel-" + UUID.randomUUID(),
                        "partnerId", partnerId,
                        "connectionId", UUID.randomUUID().toString(),
                        "type", "SFTP",
                        "direction", "OUTBOUND"
                ))
                .when().post("/api/v1/channels")
                .then()
                .statusCode(400)
                .body("error", equalTo("bad_request"))
                .body("message", containsString("Connection not found"));
    }

    @Test
    void enableDisableChannel() {
        String partnerId = createPartner();
        String connectionId = createConnection();

        // Create enabled channel
        String channelId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "toggle-channel-" + UUID.randomUUID(),
                        "partnerId", partnerId,
                        "connectionId", connectionId,
                        "type", "REST",
                        "direction", "BIDIRECTIONAL",
                        "enabled", true
                ))
                .when().post("/api/v1/channels")
                .then()
                .statusCode(201)
                .body("enabled", equalTo(true))
                .extract().path("id");

        // Disable
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "toggle-channel-disabled-" + UUID.randomUUID(),
                        "partnerId", partnerId,
                        "connectionId", connectionId,
                        "type", "REST",
                        "direction", "BIDIRECTIONAL",
                        "enabled", false
                ))
                .when().put("/api/v1/channels/" + channelId)
                .then()
                .statusCode(200)
                .body("enabled", equalTo(false));

        // Re-enable
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .body(Map.of(
                        "name", "toggle-channel-reenabled-" + UUID.randomUUID(),
                        "partnerId", partnerId,
                        "connectionId", connectionId,
                        "type", "REST",
                        "direction", "BIDIRECTIONAL",
                        "enabled", true
                ))
                .when().put("/api/v1/channels/" + channelId)
                .then()
                .statusCode(200)
                .body("enabled", equalTo(true));
    }
}
