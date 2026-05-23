# auth-storage-postgres

Placeholder documental para una fase futura. No contiene codigo ni dependencia
PostgreSQL todavia.

## Estado

- `AUTH_STORAGE_MODE=postgres` esta planificado, no implementado.
- No hay driver PostgreSQL agregado.
- No hay migraciones PostgreSQL activas.
- No hay pruebas PostgreSQL todavia.

## Alcance Futuro

El modulo deberia implementar los mismos contratos que `auth-storage-sqlite`:

- repositorio de clientes;
- registro de servicios;
- auditoria persistente;
- sesiones opacas distribuidas;
- migraciones versionadas.

La implementacion debe usar RDS PostgreSQL en AWS y secretos desde Secrets
Manager para un production-like deployment posterior.
