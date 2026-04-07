package io.qamelo.connectivity.domain.partner;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Partner {

    private final UUID id;
    private String name;
    private String description;
    private List<PartnerIdentifier> identifiers;
    private final Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;

    public Partner(UUID id, String name, String description, List<PartnerIdentifier> identifiers,
                   Instant createdAt, String createdBy, Instant modifiedAt, String modifiedBy) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        setName(name);
        this.description = description;
        this.identifiers = identifiers != null ? List.copyOf(identifiers) : List.of();
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt must not be null");
        this.createdBy = createdBy;
        this.modifiedAt = modifiedAt;
        this.modifiedBy = modifiedBy;
    }

    public UUID getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) {
        Objects.requireNonNull(name, "Name must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("Name must not be blank");
        this.name = name;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<PartnerIdentifier> getIdentifiers() { return identifiers; }

    public void setIdentifiers(List<PartnerIdentifier> identifiers) {
        this.identifiers = identifiers != null ? List.copyOf(identifiers) : List.of();
    }

    public Instant getCreatedAt() { return createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(Instant modifiedAt) { this.modifiedAt = modifiedAt; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
}
