# auth-storage-sqlite

Local SQLite persistence module for the modular path.

Includes:

- `SQLitePrincipalRepository` and `SQLiteServiceRegistry` repositories;
- `SQLiteAuditRepository` for Gateway flow events;
- versioned migrations in `scripts/sqlite/migrations/`;
- `SQLiteDemoDatabaseInitializer`;
- `SQLiteAdminCli` for clients, services, and audit queries.

It does not introduce an ORM, Docker, or an external database. Generated
databases `*.db`, `*.sqlite`, and `*.sqlite3` must not be versioned.
