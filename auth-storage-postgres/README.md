# auth-storage-postgres

Modulo PostgreSQL para Fase 20. Implementa los contratos de `auth-core` sin
ORM pesado y prepara el uso de RDS como almacenamiento cloud compartido.

## Incluye

- `PostgresPrincipalRepository`
- `PostgresServiceRegistry`
- `PostgresAuditRepository`
- `PostgresSessionRepository`
- `PostgresMigrationRunner`
- migraciones en `scripts/postgres/migrations/`

## Configuracion

```text
AUTH_STORAGE_MODE=postgres
AUTH_SESSION_STORAGE_MODE=postgres
AUTH_POSTGRES_URL=jdbc:postgresql://<host>:5432/kerberos_auth
AUTH_POSTGRES_USER=<user>
AUTH_POSTGRES_PASSWORD=<local-only-password>
AUTH_POSTGRES_SSL_MODE=require
```

En cloud, usar `AUTH_SECRET_PROVIDER=aws-secrets-manager` y
`AUTH_SECRET_POSTGRES_PASSWORD_ID` en lugar de versionar passwords.

## Pruebas

Las pruebas normales no requieren un PostgreSQL real:

```bash
mvn -pl auth-storage-postgres -am test
```

La prueba real futura queda deshabilitada por defecto:

```bash
AUTH_POSTGRES_URL=jdbc:postgresql://localhost:5432/kerberos_auth \
AUTH_POSTGRES_USER=kerberos_demo \
AUTH_POSTGRES_PASSWORD=<password> \
mvn -pl auth-storage-postgres -am -Ppostgres-it -Dpostgres.it=true test
```

No ejecutar esta prueba con credenciales reales versionadas.
