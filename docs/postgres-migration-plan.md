# PostgreSQL Migration Plan

Esta fase no implementa PostgreSQL. Deja la propuesta para un modulo futuro
`auth-storage-postgres` y para `AUTH_STORAGE_MODE=postgres`.

## Objetivo

Reemplazar SQLite en despliegues cloud por RDS PostgreSQL sin cambiar los
contratos publicos de `auth-core`.

## Modulo Futuro

Modulo sugerido:

```text
auth-storage-postgres/
```

Responsabilidades:

- Implementar `PrincipalRepository`.
- Implementar `ServiceRegistry`.
- Implementar `AuthEventRepository`.
- Implementar `SessionRepository`.
- Ejecutar migraciones versionadas.
- Proveer pruebas de integracion con PostgreSQL local o Testcontainers si se
  autoriza en una fase futura.

No agregar dependencia PostgreSQL hasta implementar el modulo real con pruebas.

## Variable De Modo

Modo futuro:

```text
AUTH_STORAGE_MODE=postgres
AUTH_SESSION_STORAGE_MODE=postgres
```

Mientras no exista el modulo, `postgres` debe tratarse como plan documentado,
no como funcionalidad activa.

## Esquema Equivalente

Tablas esperadas:

- `auth_clients`
- `auth_services`
- `audit_events`
- `auth_sessions`
- `schema_version`

Consideraciones:

- IDs textuales compatibles con SQLite.
- Timestamps en `TIMESTAMPTZ`.
- Indices por `request_id`, `client_id`, `service_id`, `event_type` y
  expiracion de sesion.
- Restricciones para sesiones revocadas y expiradas.

## Migraciones

Usar migraciones SQL versionadas equivalentes a `scripts/sqlite/migrations/`.
La fase futura debe incluir:

- migracion inicial;
- seed opcional solo para demo;
- indices de auditoria;
- tabla de sesiones opacas;
- pruebas de idempotencia.

## RDS

RDS PostgreSQL debe vivir en subnets privadas, con Security Group que permita
`5432` solo desde ECS. Habilitar backups, deletion protection, cifrado KMS,
logs y parametros revisados.

## Sesiones Opacas Distribuidas

`auth_sessions` debe ser compartida por todos los Gateways. `VERIFY_SESSION` y
`LOGOUT_SESSION` deben operar sobre el mismo repositorio distribuido para que
el escalado horizontal sea coherente.

## Auditoria Persistente

Los eventos de auditoria deben conservar el comportamiento actual:

- inicio de flujo;
- exito;
- fallo;
- duracion;
- razon de error sin secretos.

No registrar tickets, claves, ciphertexts ni secretos.

## Secretos

Los secretos de conexion y secretos operativos deben venir desde Secrets
Manager. No deben vivir en `.env`, Terraform tfvars, Docker Compose ni codigo.
