# Qamelo Connectivity

Connectivity, B2B, and Agent Management service for the Qamelo integration platform. Control plane for connections, trading partners, channels, agreements, agents, certificates, and connectivity tests.

Platform docs: https://github.com/qamelo-io/qamelo-docs
Design decisions: https://github.com/qamelo-io/qamelo-docs/blob/main/CONNECTIVITY-DESIGN.md

## Tech Stack

- Java 25, Quarkus 3.33.1 LTS
- Hibernate Reactive + Panache (PostgreSQL)
- Flyway (migrations)
- SmallRye JWT, SmallRye Health
- Vert.x WebClient (IAM client, Secrets client)
- Fabric8 Kubernetes Client (future phases)
- Testcontainers (PostgreSQL integration tests)

## Modules

| Module | Purpose |
|---|---|
| qamelo-connectivity-domain | Entities, enums, SPI interfaces, error model. Zero infra imports. |
| qamelo-connectivity-infra | PostgreSQL repositories (Hibernate Reactive), IAM client, external clients |
| qamelo-connectivity-app | REST resources, auth filters, health checks, audit, Quarkus wiring |
| qamelo-connectivity-common | Shared utilities |

## Architecture

- Hexagonal: domain defines interfaces, infra implements with PostgreSQL/HTTP
- Control plane only — traffic never flows through connectivity
- Four auth filter contexts on one port (MtlsAuthFilter, RegistrationTokenFilter, AgentCertFilter, JwtAuthFilter)
- Credentials never in PostgreSQL — stored in Vault via qamelo-secrets, only vault path refs in DB
- Responses return `hasCredentials` boolean, never vault paths or credential values

## REST API

- Admin API: `/api/v1/connections/*`, `/api/v1/partners/*`, `/api/v1/channels/*`, `/api/v1/agreements/*`, `/api/v1/agents/*`
- Agent API: `/api/v1/agents/register`, `/api/v1/agents/{id}/renew-cert`, `/api/v1/agents/{id}/config`, `/api/v1/agents/{id}/status`
- Internal API: `/api/v1/internal/*` (mTLS protected)
- Health: `/q/health/ready`, `/q/health/live`, `/api/v1/internal/connectivity/health`

## Auth

- JwtAuthFilter validates Bearer JWT from console users
- MtlsAuthFilter validates client certs on `/api/v1/internal/*`
- AuthorizationService with canOperate pattern (same as qamelo-server)
- Auth context cache (TTL 60s, fail-closed on cold cache)

## Error Model

7 codes: NOT_FOUND (404), BAD_REQUEST (400), CONFLICT (409), FORBIDDEN (403), UNAUTHORIZED (401), SERVICE_UNAVAILABLE (503), INTERNAL_ERROR (500)

## Build

```bash
mvn clean install
```

## Related Repos

- **qamelo-docs** — cross-repo architecture, CONNECTIVITY-DESIGN.md, PRD
- **qamelo-iam** — auth context API, permission seeder
- **qamelo-secrets** — credential storage (Vault broker)
- **qamelo-server** — auth pattern origin (JwtAuthFilter, canOperate)
- **qamelo-infra** — K8s deployment, Helm charts

## Version

0.1.0-SNAPSHOT
