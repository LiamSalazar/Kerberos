# auth-storage-sqlite

Modulo de persistencia local SQLite para la ruta modular.

Incluye:

- repositorios `SQLitePrincipalRepository` y `SQLiteServiceRegistry`;
- `SQLiteAuditRepository` para eventos de flujo del Gateway;
- migraciones versionadas en `scripts/sqlite/migrations/`;
- `SQLiteDemoDatabaseInitializer`;
- `SQLiteAdminCli` para clientes, servicios y consulta de auditoria.

No introduce ORM, Docker ni base externa. Las bases generadas `*.db`,
`*.sqlite` y `*.sqlite3` no deben versionarse.
