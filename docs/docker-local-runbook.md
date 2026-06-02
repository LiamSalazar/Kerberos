# Docker Local Runbook

Docker and Docker Compose are authorized here as reproducible local deployment.
They are not presented as production.

## Local SQLite

```bash
cp .env.example .env
docker compose build
docker compose up -d
docker compose ps
curl http://localhost:2801/health
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
docker compose build
docker compose up -d
docker compose ps
Invoke-RestMethod http://localhost:2801/health
```

## Local PostgreSQL

```bash
docker compose --env-file .env.postgres --profile postgres-local build
docker compose --env-file .env.postgres --profile postgres-local up -d
docker compose --env-file .env.postgres --profile postgres-local ps
curl http://localhost:2801/health
```

Expected:

- `auth-postgres`: healthy.
- `auth-as`: healthy.
- `auth-tgs`: healthy.
- `auth-service`: healthy.
- `auth-websocket-gateway`: healthy.
- `auth-web-demo`: healthy.
- `sample-login-app`: healthy.
- Gateway `/health`: `status=UP`, `storageMode=postgres`.

## Open Demos

```text
http://localhost:5173
http://localhost:5174
ws://localhost:2800
```

## Logs

```bash
docker compose logs auth-websocket-gateway
docker compose logs auth-as
docker compose logs auth-tgs
docker compose logs auth-service
docker compose logs auth-web-demo
docker compose logs sample-login-app
```

With PostgreSQL profile:

```bash
docker compose --env-file .env.postgres --profile postgres-local logs auth-websocket-gateway
```

## Rebuild

```bash
docker compose build --no-cache
docker compose up -d
```

PostgreSQL:

```bash
docker compose --env-file .env.postgres --profile postgres-local build --no-cache
docker compose --env-file .env.postgres --profile postgres-local up -d
```

## Stop

```bash
docker compose down
docker compose --env-file .env.postgres --profile postgres-local down
```

## Clear Local Volumes

This deletes local demo data:

```bash
docker compose down -v
docker compose --env-file .env.postgres --profile postgres-local down -v
```

## Occupied Ports

See processes:

Linux/macOS:

```bash
lsof -i :2800
lsof -i :2801
lsof -i :5173
lsof -i :5174
```

Windows PowerShell:

```powershell
Get-NetTCPConnection -LocalPort 2800,2801,5173,5174 -ErrorAction SilentlyContinue
```

Do not change the project ports without authorization because they are part of
the documented local contract.

## How To Know It Works

1. `docker compose ps` shows healthy services.
2. `curl http://localhost:2801/health` returns `UP`.
3. `auth-web-demo` opens at `http://localhost:5173`.
4. `sample-login-app` opens at `http://localhost:5174`.
5. Expected flow: `START_AUTH_FLOW -> FLOW_RESULT -> VERIFY_SESSION -> SESSION_VALID`.
6. Expected logout: `LOGOUT_SESSION -> SESSION_LOGGED_OUT`.
