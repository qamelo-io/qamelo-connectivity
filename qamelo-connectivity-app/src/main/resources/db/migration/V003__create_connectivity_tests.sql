CREATE TABLE connectivity_tests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    connection_id   UUID NOT NULL REFERENCES connections(id) ON DELETE CASCADE,
    direction       VARCHAR(50) NOT NULL,
    type            VARCHAR(50) NOT NULL,
    status          VARCHAR(50) NOT NULL,
    result_message  TEXT,
    latency_ms      BIGINT,
    error_detail    TEXT,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    initiated_by    VARCHAR(255)
);

CREATE INDEX idx_connectivity_tests_connection_id ON connectivity_tests (connection_id);
CREATE INDEX idx_connectivity_tests_started_at ON connectivity_tests (started_at DESC);
