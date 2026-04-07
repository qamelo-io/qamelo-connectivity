CREATE TABLE managed_certificates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    partner_id      UUID REFERENCES partners(id),
    connection_id   UUID REFERENCES connections(id),
    agent_id        UUID REFERENCES agents(id),
    usage           VARCHAR(50) NOT NULL,
    source          VARCHAR(50) NOT NULL,
    vault_path      VARCHAR(500),
    serial_number   VARCHAR(255),
    subject_dn      VARCHAR(500),
    issuer_dn       VARCHAR(500),
    not_before      TIMESTAMPTZ,
    not_after       TIMESTAMPTZ,
    status          VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    version         INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255),
    modified_at     TIMESTAMPTZ,
    modified_by     VARCHAR(255),
    CONSTRAINT chk_exactly_one_fk CHECK (
        (CASE WHEN partner_id IS NOT NULL THEN 1 ELSE 0 END +
         CASE WHEN connection_id IS NOT NULL THEN 1 ELSE 0 END +
         CASE WHEN agent_id IS NOT NULL THEN 1 ELSE 0 END) = 1
    )
);

CREATE INDEX idx_managed_certs_partner_id ON managed_certificates (partner_id);
CREATE INDEX idx_managed_certs_connection_id ON managed_certificates (connection_id);
CREATE INDEX idx_managed_certs_agent_id ON managed_certificates (agent_id);
CREATE INDEX idx_managed_certs_status ON managed_certificates (status);
CREATE INDEX idx_managed_certs_not_after ON managed_certificates (not_after);
