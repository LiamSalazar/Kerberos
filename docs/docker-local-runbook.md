# Docker Local Runbook

Docker y Docker Compose estan autorizados aqui como despliegue local
reproducible. No se presentan como produccion.

## SQLite Local

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

## PostgreSQL Local

```bash
docker compose --env-file .env.postgres --profile postgres-local build
docker compose --env-file .env.postgres --profile postgres-local up -d
docker compose --env-file .env.postgres --profile postgres-local ps
curl http://localhost:2801/health
```

Esperado:

- `auth-postgres`: healthy.
- `auth-as`: healthy.
- `auth-tgs`: healthy.
- `auth-service`: healthy.
- `auth-websocket-gateway`: healthy.
- `auth-web-demo`: healthy.
- `sample-login-app`: healthy.
- Gateway `/health`: `status=UP`, `storageMode=postgres`.

## Abrir Demos

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

Con perfil PostgreSQL:

```bash
docker compose --env-file .env.postgres --profile postgres-local logs auth-websocket-gateway
```

## Reconstruir

```bash
docker compose build --no-cache
docker compose up -d
```

PostgreSQL:

```bash
docker compose --env-file .env.postgres --profile postgres-local build --no-cache
docker compose --env-file .env.postgres --profile postgres-local up -d
```

## Apagar

```bash
docker compose down
docker compose --env-file .env.postgres --profile postgres-local down
```

## Limpiar Volumenes Locales

Esto borra datos locales de demo:

```bash
docker compose down -v
docker compose --env-file .env.postgres --profile postgres-local down -v
```

## Puertos Ocupados

Ver procesos:

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

No cambie los puertos del proyecto sin autorizacion porque son parte del
contrato local documentado.

## Como Saber Si Funciona

1. `docker compose ps` muestra servicios healthy.
2. `curl http://localhost:2801/health` devuelve `UP`.
3. `auth-web-demo` abre en `http://localhost:5173`.
4. `sample-login-app` abre en `http://localhost:5174`.
5. Flujo esperado: `START_AUTH_FLOW -> FLOW_RESULT -> VERIFY_SESSION -> SESSION_VALID`.
6. Logout esperado: `LOGOUT_SESSION -> SESSION_LOGGED_OUT`.
