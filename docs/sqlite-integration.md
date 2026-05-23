# SQLite Integration

Fase 15 convierte SQLite en una capa local mas seria: migraciones versionadas,
repositorios de clientes/servicios, auditoria persistente y administracion por
CLI. Sigue siendo una integracion local sin servidor externo y sin ORM. Docker
local es opcional para reproducibilidad, pero SQLite sigue siendo interno del
sistema de autenticacion.

SQLite no es una base productiva para despliegue critico. PostgreSQL, replicas,
vaults y migraciones avanzadas quedan fuera de esta fase.

## Modulo

`auth-storage-sqlite` implementa:

- `SQLitePrincipalRepository`
- `SQLiteServiceRegistry`
- `SQLiteAuditRepository`
- `SQLiteSessionRepository`
- `SQLiteMigrationRunner`
- `SQLiteDemoDatabaseInitializer`
- `SQLiteAdminCli`
- `SQLiteAdminRepository`

Las interfaces de runtime viven en `auth-core`:

- `PrincipalRepository`
- `ServiceRegistry`
- `AuditRepository`
- `AuthEventRepository`

## Configuracion

```text
AUTH_STORAGE_MODE=memory
AUTH_STORAGE_MODE=sqlite
AUTH_SQLITE_PATH=data/auth-demo.sqlite
AUTH_SESSION_STORAGE_MODE=sqlite
```

`memory` sigue siendo el default demo. `sqlite` activa lectura de clientes,
TGS, servicios, auditoria persistente del Gateway y sesiones opacas cuando
`AUTH_SESSION_STORAGE_MODE=sqlite`.

## Migraciones

Las migraciones versionadas viven en:

```text
scripts/sqlite/migrations/
```

Versiones actuales:

- `V1__schema.sql`: tablas `principals` y `services`.
- `V2__seed_demo.sql`: datos demo locales.
- `V3__audit_events.sql`: tabla `auth_audit_events` e indices.
- `V4__audit_query_indexes.sql`: indices de consulta de auditoria.
- `V5__auth_sessions.sql`: tabla `auth_sessions` e indices de sesion.

El migrador crea `schema_version` y registra version, descripcion, script,
checksum y fecha de aplicacion. Reaplicar migraciones no duplica versiones ni
datos demo.

Los scripts historicos `scripts/sqlite/schema.sql` y
`scripts/sqlite/seed-demo.sql` se mantienen como referencia plana, pero la ruta
recomendada es el migrador.

## Inicializar Base Demo

Windows:

```cmd
scripts\init-sqlite-demo.bat --db data\auth-demo.sqlite
```

Linux/macOS:

```bash
scripts/init-sqlite-demo.sh --db data/auth-demo.sqlite
```

Opcionalmente:

```cmd
scripts\init-sqlite-demo.bat --db data\auth-demo.sqlite --migrations scripts\sqlite\migrations
```

## Ejecutar En Modo SQLite

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

## Administracion Local

Registrar cliente:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients add --id app-client --display-name "App Client" --secret "<secret>"
```

Registrar servicio:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services add --id melodyfinder --display-name "MelodyFinder" --secret "<secret>" --endpoint local://melodyfinder
```

Listar:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients list
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services list
```

Habilitar/deshabilitar:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients disable --id app-client
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients enable --id app-client
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services disable --id melodyfinder
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services enable --id melodyfinder
```

Los listados no imprimen secretos.

## Auditoria Persistente

Cuando `auth-websocket-gateway` corre con `AUTH_STORAGE_MODE=sqlite`, registra
eventos seguros en `auth_audit_events`:

- `request_id`
- `client_id`
- `service_id`
- `event_type`
- `status`
- `error_type`
- `latency_ms`
- `created_at`

Consultar:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit list --limit 20
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-request --request-id sample-login-1
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-client --client-id 1
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-service --service-id 1
```

No se registran secretos, claves, tickets descifrados completos, ciphertexts ni
payloads internos.

## Sesiones Opacas

Cuando el Gateway corre con `AUTH_SESSION_STORAGE_MODE=sqlite`, guarda sesiones
en `auth_sessions`:

- `session_id`
- `request_id`
- `client_id`
- `service_id`
- `issued_at`
- `expires_at`
- `status`
- `revoked_at`
- `created_at`

No se guardan secretos, tickets, ciphertexts ni claves. La app externa no debe
consultar esta tabla; debe usar `VERIFY_SESSION` contra el Gateway.

## Pruebas

```bash
mvn -pl auth-storage-sqlite -am test
mvn -pl auth-client-sdk -am test
mvn -pl auth-websocket-gateway -am test
```

Cobertura agregada:

- aplicacion de migraciones en base temporal;
- no duplicar migraciones;
- seed demo verificable;
- persistencia y consulta de auditoria;
- persistencia, expiracion, revocacion y mismatch de sesiones;
- administracion de clientes y servicios;
- flujo AS -> TGS -> Service respaldado por SQLite.

## Docker

`docker-compose.yml` usa el volumen `auth-sqlite-data` y la ruta
`/data/auth-demo.sqlite`. El servicio `auth-storage-init` aplica migraciones
antes de levantar AS/TGS/Service/Gateway.

Limpiar el volumen:

```bash
docker compose down -v
```

## Dependencia

`org.xerial:sqlite-jdbc` es la unica dependencia externa nueva para SQLite. No
se agrego ORM.

## Limites

- SQLite es local y ligera.
- No hay cifrado de secretos en repositorio.
- No hay rotacion de secretos.
- No hay API HTTP de administracion.
- No hay PostgreSQL.
