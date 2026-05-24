# Production Readiness Checklist

Checklist para una fase posterior. No marca el sistema como production-ready.

## Codigo Y Build

- [x] Maven corre desde la raiz.
- [x] CI apunta a la raiz.
- [x] Docker Compose apunta a la raiz.
- [x] `auth-storage-postgres` existe en el reactor Maven.
- [ ] Docker validado en Linux.
- [ ] Imagenes publicadas en ECR con tags inmutables.
- [ ] SBOM o auditoria de dependencias automatizada.

## Seguridad

- [x] `AUTH_MODE=strict` existe para rechazar secretos demo.
- [x] `VERIFY_SESSION` existe antes de conceder acceso.
- [x] `SecretsProvider` soporta env y AWS Secrets Manager sin llamar AWS en tests normales.
- [x] Terraform referencia Secrets Manager para tareas ECS.
- [ ] Secretos reales rotados.
- [ ] TLS publico con ACM.
- [ ] mTLS o proteccion equivalente entre Gateway y AS/TGS/Service.
- [ ] Security Groups revisados por minimo privilegio.

## Persistencia

- [x] SQLite local con migraciones.
- [x] Auditoria persistente local.
- [x] PostgreSQL implementado como modulo real.
- [x] Migraciones PostgreSQL versionadas.
- [ ] RDS privado con backups y deletion protection.
- [x] Sesiones opacas soportan repositorio PostgreSQL compartido.
- [ ] Sesiones opacas distribuidas validadas contra RDS/PostgreSQL real.

## Operacion

- [x] CloudWatch Logs previsto en Terraform.
- [x] Health endpoints reales para procesos Java y ALB Gateway.
- [x] Metricas basicas en memoria y `/metrics`.
- [ ] Alarmas por errores, latencia y tareas reiniciadas.
- [ ] Runbook de incidentes.
- [ ] Pruebas de carga.
- [ ] Pruebas de recuperacion.

## AWS

- [x] Skeleton Terraform creado.
- [x] Blueprint actualizado para ALB/WSS, Gateway `/health`, Secrets Manager y RDS.
- [ ] `terraform fmt` y `terraform validate` ejecutados en entorno con Terraform.
- [ ] `terraform plan` revisado con variables reales no secretas.
- [ ] DNS y ACM configurados.
- [ ] ECR push validado.
- [ ] ECS deployment validado en cuenta sandbox.

## Decision Final

Solo llamar production-ready real despues de completar los puntos pendientes,
validar bajo carga y revisar seguridad, operacion, backup y recuperacion.
