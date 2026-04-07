CREATE TABLE partners (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255),
    modified_at TIMESTAMPTZ,
    modified_by VARCHAR(255),
    CONSTRAINT uq_partners_name UNIQUE (name)
);

CREATE TABLE partner_identifiers (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id            UUID NOT NULL REFERENCES partners(id) ON DELETE CASCADE,
    scheme                VARCHAR(50) NOT NULL,
    custom_scheme_label   VARCHAR(255),
    value                 VARCHAR(500) NOT NULL,
    CONSTRAINT uq_partner_identifier UNIQUE (partner_id, scheme, value)
);

CREATE INDEX idx_partner_identifiers_partner_id ON partner_identifiers (partner_id);
CREATE INDEX idx_partner_identifiers_scheme_value ON partner_identifiers (scheme, value);
