# Linux Docker Validation

Expected validation on a Linux machine with Docker Compose v2. These commands
start from scratch and use the repository root.

## Clone

```bash
git clone https://github.com/LiamSalazar/Kerberos.git
cd Kerberos
```

## Local Validation

```bash
mvn -q -DskipTests compile
mvn test
```

## Docker

```bash
cp .env.example .env
docker compose config
docker compose build
docker compose up
```

Optional cloud-like validation with local PostgreSQL:

```bash
AUTH_STORAGE_MODE=postgres AUTH_SESSION_STORAGE_MODE=postgres docker compose --profile postgres-local config
AUTH_STORAGE_MODE=postgres AUTH_SESSION_STORAGE_MODE=postgres docker compose --profile postgres-local build
AUTH_STORAGE_MODE=postgres AUTH_SESSION_STORAGE_MODE=postgres docker compose --profile postgres-local up
```

Open:

```text
http://localhost:5173
http://localhost:5174
```

## Smoke Test

1. Open `http://localhost:5173`.
2. Open `http://localhost:5174`.
3. Test `START_AUTH_FLOW` with `clientId=1` and `serviceId=1`.
4. Verify `FLOW_RESULT` with `sessionId`.
5. Send `VERIFY_SESSION`.
6. Verify `SESSION_VALID`.
7. Test `LOGOUT_SESSION`.
8. Query SQLite audit.

## SQLite Audit

With the containers running, inspect the volume or run the local CLI against a
database copied from the volume. For manual local validation outside Docker:

```bash
scripts/sqlite-admin.sh --db data/auth-demo.sqlite audit list --limit 20
```

The audit must not expose secrets, tickets, keys, or ciphertexts.

## Expected Result

- `docker compose config` generates normalized YAML without errors.
- `docker compose build` builds images.
- `docker compose up` starts Gateway, web demo, and sample login app publicly.
- AS, TGS, and Service do not expose ports to the host.
- The Gateway responds with a verifiable opaque session.
- Java health checks respond on `/health`.
- The `postgres-local` profile does not publish PostgreSQL to the host and uses demo values.
