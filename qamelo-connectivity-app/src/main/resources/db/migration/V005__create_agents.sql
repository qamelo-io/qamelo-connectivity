CREATE TABLE agents (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                            VARCHAR(255) NOT NULL,
    description                     TEXT,
    status                          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    registration_token              VARCHAR(500),
    registration_token_expires_at   TIMESTAMPTZ,
    registered_at                   TIMESTAMPTZ,
    cert_serial_number              VARCHAR(255),
    cert_expires_at                 TIMESTAMPTZ,
    cert_subject_san                VARCHAR(500),
    last_seen_at                    TIMESTAMPTZ,
    tunnel_remote_address           VARCHAR(255),
    k8s_namespace                   VARCHAR(255),
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                      VARCHAR(255),
    modified_at                     TIMESTAMPTZ,
    modified_by                     VARCHAR(255),
    CONSTRAINT uq_agents_name UNIQUE (name)
);

CREATE INDEX idx_agents_status ON agents (status);
CREATE INDEX idx_agents_registration_token ON agents (registration_token);
