# Cloud Production Readiness

Este proyecto no debe presentarse como production-ready empresarial. Fase 20
prepara un camino cloud-production readiness y documenta lo que falta para
endurecerlo.

## Ya Preparado

- Monorepo real en la raiz.
- Maven ejecutable desde la raiz.
- Docker Compose listo para validacion Linux.
- Gateway WebSocket con sesiones opacas verificables.
- Auditoria persistente SQLite local.
- Modulo `auth-storage-postgres` con repositorios JDBC y migraciones SQL.
- `SecretsProvider` con `env` y AWS Secrets Manager preparado.
- Health HTTP ligero y endpoint `/metrics` para procesos Java.
- Logs JSON sanitizados en el Gateway para flujo y sesiones.
- Skeleton Terraform para ECS Fargate, ECR, ALB, VPC, Security Groups,
  Secrets Manager, CloudWatch, IAM y RDS PostgreSQL opcional.
- Documentacion de despliegue y validacion.

## Pendiente Para Production-Ready Real

- Validar Docker en Linux con `docker compose config`, `build` y `up`.
- Publicar imagenes reales en ECR.
- Crear certificados ACM y DNS reales.
- Validar `auth-storage-postgres` contra PostgreSQL/RDS real.
- Cargar secretos reales en Secrets Manager fuera del repositorio.
- Agregar TLS/mTLS o proteccion equivalente para trafico interno.
- Definir backup, restore, retention y rotacion.
- Agregar observabilidad operacional completa: alarmas, dashboards y trazas.
- Hacer revision de amenazas y pruebas de carga.

## Riesgos Restantes

- El replay cache sigue siendo local por proceso.
- El modo SQLite no escala horizontalmente.
- El Gateway ya tiene health HTTP, pero falta validarlo detras de ALB real.
- El protocolo interno TCP/JSON no tiene mTLS.
- `AUTH_MODE=strict` puede resolver Secrets Manager, pero falta cargar secretos
  reales, revisar IAM y ejecutar pruebas en AWS.
- La autorizacion de negocio sigue fuera del sistema demo.

## Lenguaje Permitido

Usar:

- production-like deployment
- cloud deployment ready after validation

Evitar:

- production-ready
- enterprise-ready
- secure by default para AWS
