# AWS Deployment Readiness

Fase 19 prepara el camino para un production-like deployment en AWS, sin
desplegar recursos reales y sin usar credenciales. Esta documentacion describe
la arquitectura recomendada para validar despues de que Docker funcione en
Linux y exista una estrategia real de secretos y persistencia.

## Arquitectura Recomendada

Entrada publica:

- Application Load Balancer publico.
- Listener HTTPS `443` con certificado ACM.
- WSS hacia `auth-websocket-gateway` en ECS Fargate.
- `auth-web-demo` y `sample-login-app` como contenedores publicos detras del
  mismo ALB, idealmente por hostnames separados.

Zona privada:

- `auth-as` en ECS Fargate sin puerto publicado.
- `auth-tgs` en ECS Fargate sin puerto publicado.
- `auth-service` en ECS Fargate sin puerto publicado.
- Base de datos privada, preferentemente RDS PostgreSQL.
- Secrets Manager para secretos operativos.
- CloudWatch Logs para todos los contenedores.

## Que Va Publico

- ALB.
- Gateway WSS.
- Frontend demo.
- Sample login app.

El Gateway es el unico punto que debe hablar con AS/TGS/Service. Las apps de
browser no deben abrir conexiones a la base ni a los servicios privados.

## Que Va Privado

- AS.
- TGS.
- Service.
- Base de datos.
- Secretos.
- Trafico interno entre Gateway y servicios de autenticacion.

## WSS Con ACM Y ALB

El WebSocket local usa `ws://` solo para desarrollo. En AWS debe usarse
`wss://` terminando TLS en el ALB con ACM. El ALB enruta al target group del
Gateway en el puerto `2800`; el contenedor sigue escuchando `AUTH_WS_HOST=0.0.0.0`
y `AUTH_WS_PORT=2800`.

Antes del despliegue real, agregar un health endpoint HTTP al Gateway o validar
que el health check del ALB puede recibir una respuesta estable sin iniciar un
upgrade WebSocket.

## Variables

Variables esperadas para Gateway:

```text
AUTH_MODE=strict
AUTH_STORAGE_MODE=postgres
AUTH_SESSION_STORAGE_MODE=postgres
AUTH_REQUIRE_SESSION_VERIFY=true
AUTH_SESSION_TTL_SECONDS=300
AUTH_SESSION_MAX_TTL_SECONDS=900
AUTH_WS_HOST=0.0.0.0
AUTH_WS_PORT=2800
AUTH_ALLOWED_ORIGINS=https://demo.example.com,https://login.example.com
```

AS/TGS/Service deben recibir hosts y puertos internos por Cloud Map o variables
equivalentes. Los secretos deben venir desde Secrets Manager, no desde archivos
commiteados.

## SQLite No Es Produccion

SQLite sirve para validacion local reproducible y auditoria persistente en un
solo nodo. No es la opcion adecuada para una topologia distribuida en ECS porque:

- no ofrece concurrencia multi-instancia adecuada para escritura compartida;
- no resuelve alta disponibilidad ni replicas administradas;
- complica backups, cifrado administrado y rotacion operativa;
- no encaja bien con Gateway escalado horizontalmente.

## Migracion A RDS PostgreSQL

La ruta recomendada es crear `auth-storage-postgres` como modulo real, mantener
el contrato de repositorios de `auth-core`, crear migraciones versionadas y
usar `AUTH_STORAGE_MODE=postgres`. RDS debe vivir en subnets privadas, con
Security Group que permita `5432` solo desde ECS.

## Secrets Manager

Secrets Manager debe guardar:

- secreto de cliente demo o clientes reales;
- secreto TGS;
- secreto Service;
- URL o partes de conexion PostgreSQL;
- cualquier valor futuro de cifrado operativo.

No crear versiones de secreto con valores reales en este repositorio. En AWS,
las tareas ECS deben leer secretos por ARN con IAM minimo.

## Logs

Cada servicio debe escribir a CloudWatch Logs:

- `/ecs/<prefix>/auth-as`
- `/ecs/<prefix>/auth-tgs`
- `/ecs/<prefix>/auth-service`
- `/ecs/<prefix>/auth-websocket-gateway`
- `/ecs/<prefix>/auth-web-demo`
- `/ecs/<prefix>/sample-login-app`

Revisar errores de arranque, rechazos de sesion, fallos de origen permitido y
latencia del flujo AS -> TGS -> Service.

## Escalar Gateway

El Gateway puede escalar horizontalmente solo si las sesiones opacas se guardan
en una capa distribuida. Con SQLite local, dos tareas Gateway no comparten
estado. Para AWS se requiere PostgreSQL, Redis u otra capa server-side
compartida con expiracion y logout coherente.

## Estado

La fase deja el proyecto cloud deployment ready after validation. No lo deja
production-ready real ni aplica infraestructura.
