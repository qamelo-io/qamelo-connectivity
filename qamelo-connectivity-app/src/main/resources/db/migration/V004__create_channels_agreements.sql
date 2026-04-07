CREATE TABLE channels (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    partner_id      UUID NOT NULL REFERENCES partners(id),
    connection_id   UUID NOT NULL REFERENCES connections(id),
    type            VARCHAR(50) NOT NULL,
    direction       VARCHAR(50) NOT NULL,
    properties      TEXT,
    enabled         BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255),
    modified_at     TIMESTAMPTZ,
    modified_by     VARCHAR(255),
    CONSTRAINT uq_channels_name UNIQUE (name)
);

CREATE INDEX idx_channels_partner_id ON channels (partner_id);
CREATE INDEX idx_channels_connection_id ON channels (connection_id);
CREATE INDEX idx_channels_type ON channels (type);

CREATE TABLE agreements (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                      VARCHAR(255) NOT NULL,
    channel_id                UUID NOT NULL REFERENCES channels(id),
    document_type             VARCHAR(255) NOT NULL,
    direction                 VARCHAR(50) NOT NULL,
    package_id                VARCHAR(255),
    artifact_id               VARCHAR(255),
    retry_max_retries         INT,
    retry_backoff_seconds     INT,
    retry_backoff_multiplier  DOUBLE PRECISION,
    sla_deadline_minutes      INT,
    pmode_properties          TEXT,
    status                    VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    version                   INT NOT NULL DEFAULT 0,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                VARCHAR(255),
    modified_at               TIMESTAMPTZ,
    modified_by               VARCHAR(255),
    CONSTRAINT uq_agreements_name UNIQUE (name)
);

CREATE INDEX idx_agreements_channel_id ON agreements (channel_id);
CREATE INDEX idx_agreements_status ON agreements (status);
