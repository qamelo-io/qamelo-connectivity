CREATE TABLE agent_virtual_hosts (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id                    UUID NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
    hostname                    VARCHAR(255) NOT NULL,
    target_host                 VARCHAR(500) NOT NULL,
    target_port                 INT NOT NULL,
    protocol                    VARCHAR(50) NOT NULL DEFAULT 'TCP',
    status                      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    connection_id               UUID REFERENCES connections(id),
    circuit_breaker_threshold   INT NOT NULL DEFAULT 5,
    timeout_seconds             INT NOT NULL DEFAULT 30,
    max_concurrent_connections  INT NOT NULL DEFAULT 100,
    retry_max_attempts          INT NOT NULL DEFAULT 3,
    retry_backoff_ms            INT NOT NULL DEFAULT 1000,
    pool_max_connections        INT NOT NULL DEFAULT 10,
    pool_keep_alive_seconds     INT NOT NULL DEFAULT 60,
    pool_idle_timeout_seconds   INT NOT NULL DEFAULT 300,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                  VARCHAR(255),
    modified_at                 TIMESTAMPTZ,
    modified_by                 VARCHAR(255),
    CONSTRAINT uq_virtual_host_hostname UNIQUE (hostname)
);

CREATE INDEX idx_virtual_hosts_agent_id ON agent_virtual_hosts (agent_id);
CREATE INDEX idx_virtual_hosts_connection_id ON agent_virtual_hosts (connection_id);
