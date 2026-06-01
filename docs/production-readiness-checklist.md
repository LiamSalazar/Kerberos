# Production Readiness Checklist

Checklist para una fase posterior. No marca el sistema como production-ready.

## Codigo Y Build

- [x] Maven corre desde la raiz.
- [x] CI apunta a la raiz.
- [x] Docker Compose apunta a la raiz.
- [x] `auth-storage-postgres` existe en el reactor Maven.
- [x] Docker validado en Linux.

## Seguridad

- [x] `AUTH_MODE=strict` existe para rechazar secretos demo.
- [x] `VERIFY_SESSION` existe antes de conceder acceso.
- [x] `SecretsProvider` soporta env y AWS Secrets Manager sin llamar AWS en tests normales.
- [x] Terraform referencia Secrets Manager para tareas ECS.

## Persistencia

- [x] SQLite local con migraciones.
- [x] Auditoria persistente local.
- [x] PostgreSQL implementado como modulo real.
- [x] Migraciones PostgreSQL versionadas.
- [x] Sesiones opacas soportan repositorio PostgreSQL compartido.

## Operacion

- [x] CloudWatch Logs previsto en Terraform.
- [x] Health endpoints reales para procesos Java y ALB Gateway.
- [x] Metricas basicas en memoria y `/metrics`.

## AWS

- [x] Skeleton Terraform creado.
- [x] Blueprint actualizado para ALB/WSS, Gateway `/health`, Secrets Manager y RDS.
- [x] `terraform fmt` y `terraform validate` ejecutados en entorno con Terraform.
- [x] `terraform plan` revisado con variables reales no secretas.
- [ ] DNS y ACM configurados.
- [ ] ECR push validado.
- [ ] ECS deployment validado en cuenta sandbox.


