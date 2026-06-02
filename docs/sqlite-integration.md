# SQLite Integration

Phase 15 turns SQLite into a more serious local layer: versioned migrations,
client/service repositories, persistent audit, and CLI administration. It
remains a local integration without an external server and without an ORM. Local
Docker is optional for reproducibility, but SQLite remains internal to the
authentication system.

SQLite is not a production database for critical deployment. PostgreSQL,
replicas, vaults, and advanced migrations are outside this phase.

## Module

`auth-storage-sqlite` implements:

- `SQLitePrincipalRepository`
- `SQLiteServiceRegistry`
- `SQLiteAuditRepository`
- `SQLiteSessionRepository`
- `SQLiteMigrationRunner`
- `SQLiteDemoDatabaseInitializer`
- `SQLiteAdminCli`
- `SQLiteAdminRepository`

Runtime interfaces live in `auth-core`:

- `PrincipalRepository`
- `ServiceRegistry`
- `AuditRepository`
- `AuthEventRepository`

## Configuration

```text
AUTH_STORAGE_MODE=memory
AUTH_STORAGE_MODE=sqlite
AUTH_SQLITE_PATH=data/auth-demo.sqlite
AUTH_SESSION_STORAGE_MODE=sqlite
```

`memory` remains the default demo mode. `sqlite` enables client, TGS, service,
persistent Gateway audit, and opaque session reads when
`AUTH_SESSION_STORAGE_MODE=sqlite`.

## Migrations

Versioned migrations live in:

```text
scripts/sqlite/migrations/
```

Current versions:

- `V1__schema.sql`: `principals` and `services` tables.
- `V2__seed_demo.sql`: local demo data.
- `V3__audit_events.sql`: `auth_audit_events` table and indexes.
- `V4__audit_query_indexes.sql`: audit query indexes.
- `V5__auth_sessions.sql`: `auth_sessions` table and session indexes.

The migrator creates `schema_version` and records version, description, script,
checksum, and application date. Reapplying migrations does not duplicate
versions or demo data.

Historical scripts `scripts/sqlite/schema.sql` and
`scripts/sqlite/seed-demo.sql` are kept as flat reference, but the recommended
path is the migrator.

## Initialize Demo Database

Windows:

```cmd
scripts\init-sqlite-demo.bat --db data\auth-demo.sqlite
```

Linux/macOS:

```bash
scripts/init-sqlite-demo.sh --db data/auth-demo.sqlite
```

Optionally:

```cmd
scripts\init-sqlite-demo.bat --db data\auth-demo.sqlite --migrations scripts\sqlite\migrations
```

## Run In SQLite Mode

Windows:

```cmd
set AUTH_STORAGE_MODE=sqlite
set AUTH_SQLITE_PATH=data\auth-demo.sqlite
scripts\run-as.bat
scripts\run-tgs.bat
scripts\run-service.bat
scripts\run-websocket-gateway.bat
```

Linux/macOS:

```bash
export AUTH_STORAGE_MODE=sqlite
export AUTH_SQLITE_PATH=data/auth-demo.sqlite
scripts/run-as.sh
scripts/run-tgs.sh
scripts/run-service.sh
scripts/run-websocket-gateway.sh
```

## Local Administration

Register client:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients add --id app-client --display-name "App Client" --secret "<secret>"
```

Register service:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services add --id melodyfinder --display-name "MelodyFinder" --secret "<secret>" --endpoint local://melodyfinder
```

List:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients list
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services list
```

Enable/disable:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients disable --id app-client
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients enable --id app-client
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services disable --id melodyfinder
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services enable --id melodyfinder
```

Listings do not print secrets.

## Persistent Audit

When `auth-websocket-gateway` runs with `AUTH_STORAGE_MODE=sqlite`, it records
safe events in `auth_audit_events`:

- `request_id`
- `client_id`
- `service_id`
- `event_type`
- `status`
- `error_type`
- `latency_ms`
- `created_at`

Query:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit list --limit 20
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-request --request-id sample-login-1
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-client --client-id 1
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-service --service-id 1
```

Secrets, keys, full decrypted tickets, ciphertexts, and internal payloads are
not recorded.

## Opaque Sessions

When the Gateway runs with `AUTH_SESSION_STORAGE_MODE=sqlite`, it stores
sessions in `auth_sessions`:

- `session_id`
- `request_id`
- `client_id`
- `service_id`
- `issued_at`
- `expires_at`
- `status`
- `revoked_at`
- `created_at`

Secrets, tickets, ciphertexts, and keys are not stored. The external app must
not query this table; it must use `VERIFY_SESSION` against the Gateway.

## Tests

```bash
mvn -pl auth-storage-sqlite -am test
mvn -pl auth-client-sdk -am test
mvn -pl auth-websocket-gateway -am test
```

Added coverage:

- applying migrations to a temporary database;
- not duplicating migrations;
- verifiable demo seed;
- audit persistence and queries;
- session persistence, expiration, revocation, and mismatch;
- client and service administration;
- AS -> TGS -> Service flow backed by SQLite.

## Docker

`docker-compose.yml` uses the `auth-sqlite-data` volume and the
`/data/auth-demo.sqlite` path. The `auth-storage-init` service applies
migrations before starting AS/TGS/Service/Gateway.

Clear the volume:

```bash
docker compose down -v
```

## Dependency

`org.xerial:sqlite-jdbc` is the only new external dependency for SQLite. No ORM
was added.

## Limits

- SQLite is local and lightweight.
- There is no repository-level secret encryption.
- There is no secret rotation.
- There is no administration HTTP API.
- PostgreSQL/RDS lives in `auth-storage-postgres`; SQLite remains a local/demo
  integration.
