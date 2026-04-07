package io.qamelo.connectivity.domain.agreement;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class Agreement {

    private final UUID id;
    private String name;
    private UUID channelId;
    private String documentType;
    private AgreementDirection direction;
    private String packageId;
    private String artifactId;
    private RetryPolicy retryPolicy;
    private Integer slaDeadlineMinutes;
    private Map<String, String> pmodeProperties;
    private AgreementStatus status;
    private int version;
    private final Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;

    public Agreement(UUID id, String name, UUID channelId, String documentType,
                     AgreementDirection direction, String packageId, String artifactId,
                     RetryPolicy retryPolicy, Integer slaDeadlineMinutes,
                     Map<String, String> pmodeProperties, AgreementStatus status, int version,
                     Instant createdAt, String createdBy, Instant modifiedAt, String modifiedBy) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        setName(name);
        this.channelId = Objects.requireNonNull(channelId, "ChannelId must not be null");
        this.documentType = Objects.requireNonNull(documentType, "DocumentType must not be null");
        this.direction = Objects.requireNonNull(direction, "Direction must not be null");
        this.packageId = packageId;
        this.artifactId = artifactId;
        this.retryPolicy = retryPolicy;
        this.slaDeadlineMinutes = slaDeadlineMinutes;
        this.pmodeProperties = pmodeProperties != null ? Map.copyOf(pmodeProperties) : Map.of();
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt must not be null");
        this.createdBy = createdBy;
        this.modifiedAt = modifiedAt;
        this.modifiedBy = modifiedBy;
    }

    /**
     * Validates and applies a status transition.
     * <p>
     * Valid transitions:
     * <ul>
     *   <li>DRAFT -> ACTIVE</li>
     *   <li>ACTIVE -> SUSPENDED, TERMINATED</li>
     *   <li>SUSPENDED -> ACTIVE, TERMINATED</li>
     *   <li>TERMINATED -> (none — terminal state)</li>
     * </ul>
     */
    public void transitionTo(AgreementStatus newStatus) {
        Objects.requireNonNull(newStatus, "Target status must not be null");
        if (this.status == newStatus) {
            return; // no-op for same status
        }
        boolean valid = switch (this.status) {
            case DRAFT -> newStatus == AgreementStatus.ACTIVE;
            case ACTIVE -> newStatus == AgreementStatus.SUSPENDED || newStatus == AgreementStatus.TERMINATED;
            case SUSPENDED -> newStatus == AgreementStatus.ACTIVE || newStatus == AgreementStatus.TERMINATED;
            case TERMINATED -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "Invalid status transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
    }

    public UUID getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) {
        Objects.requireNonNull(name, "Name must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("Name must not be blank");
        this.name = name;
    }

    public UUID getChannelId() { return channelId; }
    public void setChannelId(UUID channelId) { this.channelId = Objects.requireNonNull(channelId); }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = Objects.requireNonNull(documentType); }

    public AgreementDirection getDirection() { return direction; }
    public void setDirection(AgreementDirection direction) { this.direction = Objects.requireNonNull(direction); }

    public String getPackageId() { return packageId; }
    public void setPackageId(String packageId) { this.packageId = packageId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public RetryPolicy getRetryPolicy() { return retryPolicy; }
    public void setRetryPolicy(RetryPolicy retryPolicy) { this.retryPolicy = retryPolicy; }

    public Integer getSlaDeadlineMinutes() { return slaDeadlineMinutes; }
    public void setSlaDeadlineMinutes(Integer slaDeadlineMinutes) { this.slaDeadlineMinutes = slaDeadlineMinutes; }

    public Map<String, String> getPmodeProperties() { return pmodeProperties; }
    public void setPmodeProperties(Map<String, String> pmodeProperties) {
        this.pmodeProperties = pmodeProperties != null ? Map.copyOf(pmodeProperties) : Map.of();
    }

    public AgreementStatus getStatus() { return status; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(Instant modifiedAt) { this.modifiedAt = modifiedAt; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
}
