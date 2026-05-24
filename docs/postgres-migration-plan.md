# PostgreSQL Migration Plan

Fase 20 implementa PostgreSQL readiness mediante `auth-storage-postgres` y
habilita `AUTH_STORAGE_MODE=postgres`.

## Objetivo

Reemplazar SQLite en despliegues cloud por RDS PostgreSQL sin cambiar los
contratos publicos de `auth-core`.

## Modulo Futuro

Modulo implementado:

```text
auth-storage-postgres/
```

Responsabilidades:

- Implementar `PrincipalRepository`.
- Implementar `ServiceRegistry`.
- Implementar `AuthEventRepository`.
- Implementar `SessionRepository`.
- Ejecutar migraciones versionadas.
- Proveer pruebas normales sin PostgreSQL externo.
- Dejar una prueba real futura deshabilitada por defecto con perfil
  `postgres-it`.

## Variable De Modo

Modo:

```text
AUTH_STORAGE_MODE=postgres
AUTH_SESSION_STORAGE_MODE=postgres
```

`postgres` queda activo a nivel de configuracion y repositorios. La validacion
contra RDS/AWS real sigue pendiente.

## Esquema Equivalente

Tablas esperadas:

- `principals`
- `services`
- `auth_audit_events`
- `auth_sessions`
- `schema_version`

Consideraciones:

- IDs textuales compatibles con SQLite.
- Timestamps en `TIMESTAMPTZ`.
- Indices por `request_id`, `client_id`, `service_id`, `event_type` y
  expiracion de sesion.
- Restricciones para sesiones revocadas y expiradas.

## Migraciones

Las migraciones SQL versionadas viven en `scripts/postgres/migrations/` e
incluyen:

- migracion inicial;
- seed opcional solo para demo;
- indices de auditoria;
- tabla de sesiones opacas;
- indices de sesiones.

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
