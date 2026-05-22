# SQLite Integration

Fase 15 convierte SQLite en una capa local mas seria: migraciones versionadas,
repositorios de clientes/servicios, auditoria persistente y administracion por
CLI. Sigue siendo una integracion local sin Docker, sin servidor externo y sin
ORM.

SQLite no es una base productiva para despliegue critico. PostgreSQL, replicas,
vaults y migraciones avanzadas quedan fuera de esta fase.

## Modulo

`auth-storage-sqlite` implementa:

- `SQLitePrincipalRepository`
- `SQLiteServiceRegistry`
- `SQLiteAuditRepository`
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
```

`memory` sigue siendo el default demo. `sqlite` activa lectura de clientes,
TGS, servicios y auditoria persistente del Gateway.

## Migraciones

Las migraciones versionadas viven en:

```text
scripts/sqlite/migrations/
```

Versiones actuales:

- `V1__schema.sql`: tablas `principals` y `services`.
- `V2__seed_demo.sql`: datos demo locales.
- `V3__audit_events.sql`: tabla `auth_audit_events` e indices.

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
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit list --request-id sample-login-1
```

No se registran secretos, claves, tickets descifrados completos, ciphertexts ni
payloads internos.

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
- administracion de clientes y servicios;
- flujo AS -> TGS -> Service respaldado por SQLite.

## Dependencia

`org.xerial:sqlite-jdbc` es la unica dependencia externa nueva para SQLite. No
se agrego ORM.

## Limites

- SQLite es local y ligera.
- No hay cifrado de secretos en repositorio.
- No hay rotacion de secretos.
- No hay API HTTP de administracion.
- No hay PostgreSQL ni Docker.
