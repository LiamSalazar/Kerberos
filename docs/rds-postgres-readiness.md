# RDS PostgreSQL Readiness

Phase 20 implements `auth-storage-postgres` as the recommended cloud mode for
shared persistence. SQLite remains for demo/local use, and `memory` remains for
development.

## Modes

```text
AUTH_STORAGE_MODE=memory|sqlite|postgres
AUTH_SESSION_STORAGE_MODE=memory|sqlite|postgres
```

If `AUTH_SESSION_STORAGE_MODE` is not defined, it uses `AUTH_STORAGE_MODE`.

## PostgreSQL Variables

```text
AUTH_POSTGRES_URL=jdbc:postgresql://<host>:5432/kerberos_auth
AUTH_POSTGRES_USER=<user>
AUTH_POSTGRES_PASSWORD=<local-only-password>
AUTH_POSTGRES_SSL_MODE=require
```

In AWS, resolve the password with:

```text
AUTH_SECRET_PROVIDER=aws-secrets-manager
AUTH_SECRET_POSTGRES_PASSWORD_ID=<secret-name-or-arn>
```

## Migrations

Migrations live in:

```text
scripts/postgres/migrations/
```

They include:

- `principals`
- `services`
- `auth_audit_events`
- `auth_sessions`
- `schema_version`

Normal tests validate the presence and conceptual compatibility of the scripts
without requiring external PostgreSQL.

## RDS

RDS must live in private subnets, without a public IP, with a Security Group
that allows `5432` only from ECS. Before real AWS usage, backups, retention,
encryption, parameters, secret rotation, and restore tests still need
validation.

## Opaque Sessions

For multiple Gateway instances, use `AUTH_SESSION_STORAGE_MODE=postgres`.
`memory` does not share sessions between replicas, and SQLite is not
recommended for cloud scaling.
