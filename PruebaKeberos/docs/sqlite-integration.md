# SQLite Integration

Fase 14 agrega `auth-storage-sqlite` como primera integracion persistente local.
SQLite se usa para demostrar reemplazo de repositorios en memoria sin Docker,
sin ORM y sin servidor externo.

No es una base productiva para despliegue critico. PostgreSQL, replicas,
migraciones avanzadas y vaults quedan fuera de esta fase.

## Modulo

`auth-storage-sqlite` implementa:

- `SQLitePrincipalRepository`
- `SQLiteServiceRegistry`
- `SQLiteDemoDatabaseInitializer`
- `SqlScriptRunner`

Las interfaces viven en `auth-core`:

- `PrincipalRepository`
- `ServiceRegistry`

AS, TGS y Service seleccionan implementacion por configuracion.

## Configuracion

Variables:

- `AUTH_STORAGE_MODE=memory|sqlite`
- `AUTH_SQLITE_PATH=data/auth-demo.sqlite`

El modo por defecto sigue siendo:

```text
AUTH_STORAGE_MODE=memory
```

## Inicializar Base Demo

Windows:

```cmd
scripts\init-sqlite-demo.bat --db data\auth-demo.sqlite
```

Linux/macOS:

```bash
scripts/init-sqlite-demo.sh --db data/auth-demo.sqlite
```

El esquema vive en:

- `scripts/sqlite/schema.sql`

La semilla demo vive en:

- `scripts/sqlite/seed-demo.sql`

Las bases generadas `*.db`, `*.sqlite` y `*.sqlite3` estan ignoradas por Git.

## Ejecutar En Modo SQLite

Primero inicializa la base. Luego define las variables en las terminales donde
levantes AS, TGS y Service.

Windows:

```cmd
set AUTH_STORAGE_MODE=sqlite
set AUTH_SQLITE_PATH=data\auth-demo.sqlite
scripts\run-as.bat
```

Repite las mismas variables para:

```cmd
scripts\run-tgs.bat
scripts\run-service.bat
```

El cliente puede seguir usando:

```cmd
scripts\run-client.bat
```

## Pruebas

Pruebas del modulo SQLite:

```bash
mvn -pl auth-storage-sqlite -am test
```

Prueba de flujo completo respaldado por SQLite:

```bash
mvn -pl auth-client-sdk -am test
```

`SQLiteBackedAuthFlowIntegrationTest` crea una base temporal, ejecuta schema y
seed demo, levanta AS/TGS/Service con repositorios SQLite y valida el flujo
AS -> TGS -> Service.

## Dependencia

Se agrego `org.xerial:sqlite-jdbc` como unica dependencia SQLite. La razon es
permitir una base local embebida, reproducible y sin servidor externo. No se
agrego ORM.

## Limites

- `auth_audit_events` existe en el esquema como base para auditoria futura, pero
  no hay `AuditRepository` persistente completo en esta fase.
- La semilla demo contiene credenciales locales de demostracion; no usarla para
  entornos reales.
- No hay migrador versionado de schema todavia.
- No hay PostgreSQL ni Docker.
