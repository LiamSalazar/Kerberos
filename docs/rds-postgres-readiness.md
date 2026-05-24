# RDS PostgreSQL Readiness

Fase 20 implementa `auth-storage-postgres` como modo cloud recomendado para
persistencia compartida. SQLite se mantiene para demo/local y `memory` se
mantiene para desarrollo.

## Modos

```text
AUTH_STORAGE_MODE=memory|sqlite|postgres
AUTH_SESSION_STORAGE_MODE=memory|sqlite|postgres
```

Si `AUTH_SESSION_STORAGE_MODE` no se define, usa `AUTH_STORAGE_MODE`.

## Variables PostgreSQL

```text
AUTH_POSTGRES_URL=jdbc:postgresql://<host>:5432/kerberos_auth
AUTH_POSTGRES_USER=<user>
AUTH_POSTGRES_PASSWORD=<local-only-password>
AUTH_POSTGRES_SSL_MODE=require
```

En AWS, resolver el password con:

```text
AUTH_SECRET_PROVIDER=aws-secrets-manager
AUTH_SECRET_POSTGRES_PASSWORD_ID=<secret-name-or-arn>
```

## Migraciones

Las migraciones viven en:

```text
scripts/postgres/migrations/
```

Incluyen:

- `principals`
- `services`
- `auth_audit_events`
- `auth_sessions`
- `schema_version`

Las pruebas normales validan la presencia y compatibilidad conceptual de los
scripts sin requerir un PostgreSQL externo.

## RDS

RDS debe vivir en subnets privadas, sin IP publica, con Security Group que
permita `5432` solo desde ECS. Antes de AWS real faltan validar backups,
retention, cifrado, parametros, rotacion de secreto y pruebas de restauracion.

## Sesiones Opacas

Para multiples instancias del Gateway, usar `AUTH_SESSION_STORAGE_MODE=postgres`.
`memory` no comparte sesiones entre replicas y SQLite no es recomendado para
escalado cloud.
