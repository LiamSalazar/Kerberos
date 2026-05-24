# Linux Docker Validation

Validacion esperada en una maquina Linux con Docker Compose v2. Estos comandos
parten de cero y usan la raiz del repositorio.

## Clonar

```bash
git clone https://github.com/LiamSalazar/Kerberos.git
cd Kerberos
```

## Validacion Local

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

Validacion cloud-like opcional con PostgreSQL local:

```bash
AUTH_STORAGE_MODE=postgres AUTH_SESSION_STORAGE_MODE=postgres docker compose --profile postgres-local config
AUTH_STORAGE_MODE=postgres AUTH_SESSION_STORAGE_MODE=postgres docker compose --profile postgres-local build
AUTH_STORAGE_MODE=postgres AUTH_SESSION_STORAGE_MODE=postgres docker compose --profile postgres-local up
```

Abrir:

```text
http://localhost:5173
http://localhost:5174
```

## Smoke Test

1. Abrir `http://localhost:5173`.
2. Abrir `http://localhost:5174`.
3. Probar `START_AUTH_FLOW` con `clientId=1` y `serviceId=1`.
4. Verificar `FLOW_RESULT` con `sessionId`.
5. Enviar `VERIFY_SESSION`.
6. Verificar `SESSION_VALID`.
7. Probar `LOGOUT_SESSION`.
8. Consultar auditoria SQLite.

## Auditoria SQLite

Con los contenedores arriba, inspeccionar el volumen o ejecutar el CLI local
contra una base copiada desde el volumen. Para validacion manual local fuera de
Docker:

```bash
scripts/sqlite-admin.sh --db data/auth-demo.sqlite audit list --limit 20
```

La auditoria no debe exponer secretos, tickets, claves ni ciphertexts.

## Resultado Esperado

- `docker compose config` genera YAML normalizado sin errores.
- `docker compose build` construye imagenes.
- `docker compose up` levanta Gateway, web demo y sample login app publicos.
- AS, TGS y Service no exponen puertos al host.
- El Gateway responde con sesion opaca verificable.
- Los health checks Java responden en `/health`.
- El perfil `postgres-local` no publica PostgreSQL al host y usa valores demo.
