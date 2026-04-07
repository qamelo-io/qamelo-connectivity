package io.qamelo.connectivity.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "partner_identifiers")
public class PartnerIdentifierEntity {

    @Id
    public UUID id;

    @Column(name = "partner_id", nullable = false)
    public UUID partnerId;

    @Column(nullable = false)
    public String scheme;

    @Column(name = "custom_scheme_label")
    public String customSchemeLabel;

    @Column(nullable = false)
    public String value;
}
