# Docker Deployment

Fase 20 mantiene Docker Compose listo para validacion en Linux desde la raiz del
repositorio. No es un despliegue productivo ni reemplaza la ejecucion local sin
Docker.

## Requisitos

- Docker Desktop con Docker Compose v2.
- Java y Maven solo si tambien se quiere ejecutar sin Docker.

## Preparar Variables

Copiar el ejemplo:

```cmd
copy .env.example .env
```

Linux/macOS:

```bash
cp .env.example .env
```

`.env.example` usa `AUTH_MODE=demo`, `AUTH_STORAGE_MODE=sqlite` y no contiene
secretos reales.

Tambien define sesiones opacas del Gateway:

```text
AUTH_SESSION_TTL_SECONDS=300
AUTH_SESSION_MAX_TTL_SECONDS=900
AUTH_SESSION_STORAGE_MODE=sqlite
AUTH_REQUIRE_SESSION_VERIFY=true
AUTH_SECRET_PROVIDER=env
AUTH_POSTGRES_URL=jdbc:postgresql://auth-postgres:5432/kerberos_auth
```

## Levantar Todo

Windows:

```cmd
scripts\docker-up.bat
```

Linux/macOS:

```bash
scripts/docker-up.sh
```

Equivalente:

```bash
docker compose up -d --build
```

Compose levanta:

- `auth-storage-init`: aplica migraciones SQLite en el volumen.
- `auth-as`: interno, puerto 2000 no publicado.
- `auth-tgs`: interno, puerto 2001 no publicado.
- `auth-service`: interno, puerto 2002 no publicado.
- `auth-websocket-gateway`: publico en `ws://localhost:2800`.
- `auth-web-demo`: publico en `http://localhost:5173`.
- `sample-login-app`: publico en `http://localhost:5174`.

Opcionalmente se puede habilitar un PostgreSQL local no publico:

```bash
AUTH_STORAGE_MODE=postgres AUTH_SESSION_STORAGE_MODE=postgres docker compose --profile postgres-local up -d --build
```

Ese modo usa credenciales demo de `.env` y existe solo para validacion
cloud-like local. La base no expone puertos al host.

## Abrir Frontends

Demo tecnica:

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

## Probar START_AUTH_FLOW

Desde una herramienta WebSocket, enviar:

```json
{"type":"START_AUTH_FLOW","requestId":"docker-smoke-1","clientId":"1","serviceId":"1"}
```

Resultado esperado:

- eventos `FLOW_EVENT`;
- `FLOW_RESULT` con `success=true`, `sessionId` y `sessionExpiresAt`;
- sin secretos, tickets, claves ni ciphertexts.

Luego verificar la sesion:

```json
{"type":"VERIFY_SESSION","requestId":"docker-smoke-verify","sessionId":"<sessionId>","clientId":"1","serviceId":"1"}
```

Resultado esperado:

- `SESSION_VALID` con `valid=true`;
- `expiresAt` no mayor que la vigencia del flujo de servicio.

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

## Bajar Servicios

```cmd
scripts\docker-down.bat
```

Linux/macOS:

```bash
scripts/docker-down.sh
```

## Limpiar Volumen SQLite

Esto borra la base demo persistida por Docker:

```bash
docker compose down -v
```

## Health Checks

AS, TGS, Service y Gateway usan health checks HTTP reales:

- Gateway: `http://127.0.0.1:2801/health`
- AS: `http://127.0.0.1:2900/health`
- TGS: `http://127.0.0.1:2901/health`
- Service: `http://127.0.0.1:2902/health`

Los frontends usan una solicitud HTTP local. Los health checks no imprimen
secretos ni exponen tickets o payloads sensibles.

## Limitaciones

- No hay TLS/mTLS.
- No hay gestion de secretos con vault.
- PostgreSQL local es opcional y no reemplaza RDS.
- No hay escalado ni alta disponibilidad.
- No se debe presentar como production-ready.

## Validacion Requerida

Si `docker compose` no esta disponible en el entorno actual, no se debe inventar
resultado. La validacion queda pendiente para una maquina Linux o Docker
Desktop:

```bash
docker compose config
docker compose build
docker compose up
```

En Linux, la validacion recomendada para Fase 20 es:

```bash
cp .env.example .env
docker compose config
docker compose build
docker compose up
```
