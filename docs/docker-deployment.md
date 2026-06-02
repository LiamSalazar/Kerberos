# Docker Deployment

Phase 20 keeps Docker Compose ready for validation on Linux from the repository
root. It is not a production deployment and does not replace local execution
without Docker.

## Requirements

- Docker Desktop with Docker Compose v2.
- Java and Maven only if you also want to run without Docker.

## Prepare Variables

Copy the example:

```cmd
copy .env.example .env
```

Linux/macOS:

```bash
cp .env.example .env
```

`.env.example` uses `AUTH_MODE=demo`, `AUTH_STORAGE_MODE=sqlite`, and contains
no real secrets.

It also defines Gateway opaque sessions:

```text
AUTH_SESSION_TTL_SECONDS=300
AUTH_SESSION_MAX_TTL_SECONDS=900
AUTH_SESSION_STORAGE_MODE=sqlite
AUTH_REQUIRE_SESSION_VERIFY=true
AUTH_SECRET_PROVIDER=env
AUTH_POSTGRES_URL=jdbc:postgresql://auth-postgres:5432/kerberos_auth
```

## Start Everything

Windows:

```cmd
scripts\docker-up.bat
```

Linux/macOS:

```bash
scripts/docker-up.sh
```

Equivalent:

```bash
docker compose up -d --build
```

Compose starts:

- `auth-storage-init`: applies SQLite migrations in the volume.
- `auth-as`: internal, port 2000 not published.
- `auth-tgs`: internal, port 2001 not published.
- `auth-service`: internal, port 2002 not published.
- `auth-websocket-gateway`: public on `ws://localhost:2800`.
- `auth-web-demo`: public on `http://localhost:5173`.
- `sample-login-app`: public on `http://localhost:5174`.

Optionally, a non-public local PostgreSQL can be enabled:

```bash
AUTH_STORAGE_MODE=postgres AUTH_SESSION_STORAGE_MODE=postgres docker compose --profile postgres-local up -d --build
```

That mode uses demo credentials from `.env` and exists only for local
cloud-like validation. The database does not expose ports to the host.

## Open Frontends

Technical demo:

```text
http://localhost:5173
```

Sample login app:

```text
http://localhost:5174
```

Gateway:

```text
ws://localhost:2800
```

## Test START_AUTH_FLOW

From a WebSocket tool, send:

```json
{"type":"START_AUTH_FLOW","requestId":"docker-smoke-1","clientId":"1","serviceId":"1"}
```

Expected result:

- `FLOW_EVENT` events;
- `FLOW_RESULT` with `success=true`, `sessionId`, and `sessionExpiresAt`;
- no secrets, tickets, keys, or ciphertexts.

Then verify the session:

```json
{"type":"VERIFY_SESSION","requestId":"docker-smoke-verify","sessionId":"<sessionId>","clientId":"1","serviceId":"1"}
```

Expected result:

- `SESSION_VALID` with `valid=true`;
- `expiresAt` no later than the service flow lifetime.

## Logs

Windows:

```cmd
scripts\docker-logs.bat
scripts\docker-logs.bat auth-websocket-gateway
```

Linux/macOS:

```bash
scripts/docker-logs.sh
scripts/docker-logs.sh auth-websocket-gateway
```

## Stop Services

```cmd
scripts\docker-down.bat
```

Linux/macOS:

```bash
scripts/docker-down.sh
```

## Clear SQLite Volume

This deletes the demo database persisted by Docker:

```bash
docker compose down -v
```

## Health Checks

AS, TGS, Service, and Gateway use real HTTP health checks:

- Gateway: `http://127.0.0.1:2801/health`
- AS: `http://127.0.0.1:2900/health`
- TGS: `http://127.0.0.1:2901/health`
- Service: `http://127.0.0.1:2902/health`

The frontends use a local HTTP request. Health checks do not print secrets or
expose tickets or sensitive payloads.

## Limitations

- No TLS/mTLS.
- No vault-based secrets management.
- Local PostgreSQL is optional and does not replace RDS.
- No scaling or high availability.
- It must not be presented as production-ready.

## Required Validation

If `docker compose` is not available in the current environment, do not invent
a result. Validation remains pending for a Linux machine or Docker Desktop:

```bash
docker compose config
docker compose build
docker compose up
```

On Linux, the recommended validation for Phase 20 is:

```bash
cp .env.example .env
docker compose config
docker compose build
docker compose up
```
