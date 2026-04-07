package io.qamelo.connectivity.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agents")
public class AgentEntity {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String name;

    public String description;

    @Column(nullable = false)
    public String status;

    @Column(name = "registration_token")
    public String registrationToken;

    @Column(name = "registration_token_expires_at")
    public Instant registrationTokenExpiresAt;

    @Column(name = "registered_at")
    public Instant registeredAt;

    @Column(name = "cert_serial_number")
    public String certSerialNumber;

    @Column(name = "cert_expires_at")
    public Instant certExpiresAt;

    @Column(name = "cert_subject_san")
    public String certSubjectSan;

    @Column(name = "last_seen_at")
    public Instant lastSeenAt;

    @Column(name = "tunnel_remote_address")
    public String tunnelRemoteAddress;

    @Column(name = "k8s_namespace")
    public String k8sNamespace;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "created_by")
    public String createdBy;

    @Column(name = "modified_at")
    public Instant modifiedAt;

    @Column(name = "modified_by")
    public String modifiedBy;
}
