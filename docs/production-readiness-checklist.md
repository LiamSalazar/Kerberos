# Production Readiness Checklist

Checklist para una fase posterior. No marca el sistema como production-ready.

## Codigo Y Build

- [x] Maven corre desde la raiz.
- [x] CI apunta a la raiz.
- [x] Docker Compose apunta a la raiz.
- [ ] Docker validado en Linux.
- [ ] Imagenes publicadas en ECR con tags inmutables.
- [ ] SBOM o auditoria de dependencias automatizada.

## Seguridad

- [x] `AUTH_MODE=strict` existe para rechazar secretos demo.
- [x] `VERIFY_SESSION` existe antes de conceder acceso.
- [ ] Secrets Manager integrado en tareas ECS.
- [ ] Secretos reales rotados.
- [ ] TLS publico con ACM.
- [ ] mTLS o proteccion equivalente entre Gateway y AS/TGS/Service.
- [ ] Security Groups revisados por minimo privilegio.

## Persistencia

- [x] SQLite local con migraciones.
- [x] Auditoria persistente local.
- [ ] PostgreSQL implementado como modulo real.
- [ ] Migraciones PostgreSQL versionadas.
- [ ] RDS privado con backups y deletion protection.
- [ ] Sesiones opacas distribuidas.

## Operacion

- [x] CloudWatch Logs previsto en Terraform.
- [ ] Health endpoints reales.
- [ ] Alarmas por errores, latencia y tareas reiniciadas.
- [ ] Runbook de incidentes.
- [ ] Pruebas de carga.
- [ ] Pruebas de recuperacion.

## AWS

- [x] Skeleton Terraform creado.
- [ ] `terraform fmt` y `terraform validate` ejecutados en entorno con Terraform.
- [ ] `terraform plan` revisado con variables reales no secretas.
- [ ] DNS y ACM configurados.
- [ ] ECR push validado.
- [ ] ECS deployment validado en cuenta sandbox.

## Decision Final

Solo llamar production-ready real despues de completar los puntos pendientes,
validar bajo carga y revisar seguridad, operacion, backup y recuperacion.
