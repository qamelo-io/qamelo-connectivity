package io.qamelo.connectivity.domain.test;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ConnectivityTest {

    private final UUID id;
    private UUID connectionId;
    private TestDirection direction;
    private TestType type;
    private TestStatus status;
    private String resultMessage;
    private Long latencyMs;
    private String errorDetail;
    private Instant startedAt;
    private Instant completedAt;
    private String initiatedBy;

    public ConnectivityTest(UUID id, UUID connectionId, TestDirection direction, TestType type,
                            TestStatus status, String resultMessage, Long latencyMs, String errorDetail,
                            Instant startedAt, Instant completedAt, String initiatedBy) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        this.connectionId = Objects.requireNonNull(connectionId, "Connection ID must not be null");
        this.direction = Objects.requireNonNull(direction, "Direction must not be null");
        this.type = Objects.requireNonNull(type, "Type must not be null");
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.resultMessage = resultMessage;
        this.latencyMs = latencyMs;
        this.errorDetail = errorDetail;
        this.startedAt = Objects.requireNonNull(startedAt, "StartedAt must not be null");
        this.completedAt = completedAt;
        this.initiatedBy = initiatedBy;
    }

    public UUID getId() { return id; }

    public UUID getConnectionId() { return connectionId; }
    public void setConnectionId(UUID connectionId) { this.connectionId = Objects.requireNonNull(connectionId); }

    public TestDirection getDirection() { return direction; }
    public void setDirection(TestDirection direction) { this.direction = Objects.requireNonNull(direction); }

    public TestType getType() { return type; }
    public void setType(TestType type) { this.type = Objects.requireNonNull(type); }

    public TestStatus getStatus() { return status; }
    public void setStatus(TestStatus status) { this.status = Objects.requireNonNull(status); }

    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    public String getErrorDetail() { return errorDetail; }
    public void setErrorDetail(String errorDetail) { this.errorDetail = errorDetail; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = Objects.requireNonNull(startedAt); }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }
}
