# PostgreSQL Migration Plan

Phase 20 implements PostgreSQL readiness through `auth-storage-postgres` and
enables `AUTH_STORAGE_MODE=postgres`.

## Objective

Replace SQLite in cloud deployments with RDS PostgreSQL without changing the
public contracts in `auth-core`.

## Future Module

Implemented module:

```text
auth-storage-postgres/
```

Responsibilities:

- Implement `PrincipalRepository`.
- Implement `ServiceRegistry`.
- Implement `AuthEventRepository`.
- Implement `SessionRepository`.
- Run versioned migrations.
- Provide normal tests without external PostgreSQL.
- Leave a future real test disabled by default with the `postgres-it` profile.

## Mode Variable

Mode:

```text
AUTH_STORAGE_MODE=postgres
AUTH_SESSION_STORAGE_MODE=postgres
```

`postgres` is active at configuration and repository level. Validation against
real RDS/AWS is still pending.

## Equivalent Schema

Expected tables:

- `principals`
- `services`
- `auth_audit_events`
- `auth_sessions`
- `schema_version`

Considerations:

- Text IDs compatible with SQLite.
- Timestamps in `TIMESTAMPTZ`.
- Indexes by `request_id`, `client_id`, `service_id`, `event_type`, and session
  expiration.
- Constraints for revoked and expired sessions.

## Migrations

Versioned SQL migrations live in `scripts/postgres/migrations/` and include:

- initial migration;
- optional demo-only seed;
- audit indexes;
- opaque session table;
- session indexes.

## RDS

RDS PostgreSQL must live in private subnets, with a Security Group that allows
`5432` only from ECS. Enable backups, deletion protection, KMS encryption, logs,
and reviewed parameters.

## Distributed Opaque Sessions

`auth_sessions` must be shared by all Gateways. `VERIFY_SESSION` and
`LOGOUT_SESSION` must operate on the same distributed repository so horizontal
scaling remains coherent.

## Persistent Audit

Audit events must preserve current behavior:

- flow start;
- success;
- failure;
- duration;
- error reason without secrets.

Do not record tickets, keys, ciphertexts, or secrets.

## Secrets

Connection secrets and operational secrets must come from Secrets Manager. They
must not live in `.env`, Terraform tfvars, Docker Compose, or code.
