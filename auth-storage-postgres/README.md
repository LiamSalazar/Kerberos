# auth-storage-postgres

PostgreSQL module for Phase 20. It implements the `auth-core` contracts without
a heavy ORM and prepares RDS usage as shared cloud storage.

## Includes

- `PostgresPrincipalRepository`
- `PostgresServiceRegistry`
- `PostgresAuditRepository`
- `PostgresSessionRepository`
- `PostgresMigrationRunner`
- migrations in `scripts/postgres/migrations/`

## Configuration

```text
AUTH_STORAGE_MODE=postgres
AUTH_SESSION_STORAGE_MODE=postgres
AUTH_POSTGRES_URL=jdbc:postgresql://<host>:5432/kerberos_auth
AUTH_POSTGRES_USER=<user>
AUTH_POSTGRES_PASSWORD=<local-only-password>
AUTH_POSTGRES_SSL_MODE=require
```

In cloud environments, use `AUTH_SECRET_PROVIDER=aws-secrets-manager` and
`AUTH_SECRET_POSTGRES_PASSWORD_ID` instead of versioning passwords.

## Tests

Normal tests do not require a real PostgreSQL instance:

```bash
mvn -pl auth-storage-postgres -am test
```

The future real integration test remains disabled by default:

```bash
AUTH_POSTGRES_URL=jdbc:postgresql://localhost:5432/kerberos_auth \
AUTH_POSTGRES_USER=kerberos_demo \
AUTH_POSTGRES_PASSWORD=<password> \
mvn -pl auth-storage-postgres -am -Ppostgres-it -Dpostgres.it=true test
```

Do not run this test with versioned real credentials.
