# Cloud Production Readiness

Este proyecto no debe presentarse como production-ready empresarial. Fase 19
solo prepara un camino cloud y documenta lo que falta para endurecerlo.

## Ya Preparado

- Monorepo real en la raiz.
- Maven ejecutable desde la raiz.
- Docker Compose listo para validacion Linux.
- Gateway WebSocket con sesiones opacas verificables.
- Auditoria persistente SQLite local.
- Skeleton Terraform para ECS Fargate, ECR, ALB, VPC, Security Groups,
  Secrets Manager, CloudWatch, IAM y RDS PostgreSQL opcional.
- Documentacion de despliegue y validacion.

## Pendiente Para Production-Ready Real

- Validar Docker en Linux con `docker compose config`, `build` y `up`.
- Publicar imagenes reales en ECR.
- Crear certificados ACM y DNS reales.
- Implementar `auth-storage-postgres` con pruebas.
- Migrar sesiones opacas a almacenamiento distribuido.
- Sacar secretos demo del runtime y usar Secrets Manager.
- Agregar TLS/mTLS o proteccion equivalente para trafico interno.
- Agregar health endpoints reales para ALB/ECS.
- Definir backup, restore, retention y rotacion.
- Agregar observabilidad operacional: metricas, alarmas y trazas.
- Hacer revision de amenazas y pruebas de carga.

## Riesgos Restantes

- El replay cache sigue siendo local por proceso.
- El modo SQLite no escala horizontalmente.
- El Gateway necesita health endpoint HTTP antes de un despliegue ALB serio.
- El protocolo interno TCP/JSON no tiene mTLS.
- `AUTH_MODE=strict` exige secretos explicitos, pero todavia falta integracion
  final con Secrets Manager.
- La autorizacion de negocio sigue fuera del sistema demo.

## Lenguaje Permitido

Usar:

- production-like deployment
- cloud deployment ready after validation

Evitar:

- production-ready
- enterprise-ready
- secure by default para AWS
