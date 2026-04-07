CREATE TABLE connections (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(255)    NOT NULL,
    type                    VARCHAR(50)     NOT NULL,
    host                    VARCHAR(500)    NOT NULL,
    port                    INT             NOT NULL,
    auth_type               VARCHAR(50)     NOT NULL,
    vault_credential_path   VARCHAR(500),
    cert_management         VARCHAR(50),
    vault_client_cert_path  VARCHAR(500),
    vault_trust_store_path  VARCHAR(500),
    agent_id                UUID,
    properties              TEXT,
    description             TEXT,
    status                  VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255),
    modified_at             TIMESTAMPTZ,
    modified_by             VARCHAR(255),
    CONSTRAINT uq_connections_name UNIQUE (name)
);

CREATE INDEX idx_connections_type ON connections (type);
CREATE INDEX idx_connections_status ON connections (status);
CREATE INDEX idx_connections_agent_id ON connections (agent_id);
