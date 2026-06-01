# Technical Deep Dive

## Arranque De Servicios

- `auth-as`: ejecuta `AuthenticationServerApp` y escucha solicitudes AS por TCP/JSON.
- `auth-tgs`: ejecuta `TicketGrantingServerApp` y procesa solicitudes TGS.
- `auth-service`: ejecuta `ProtectedServiceApp` y protege el recurso demo.
- `auth-websocket-gateway`: ejecuta `WebSocketGatewayApp`, expone WebSocket `2800` y health `2801`.
- `auth-web-demo`: sirve HTML/CSS/JS vanilla en `5173`.
- `sample-login-app`: sirve MelodyFinder en `5174`.

## Modulos

- `auth-core`: contratos, DTOs, configuracion, replay cache, sesiones, health,
  logs estructurados y metricas.
- `auth-crypto`: AES-GCM, `CryptoEnvelope`, derivacion y material de sesion.
- `auth-transport`: `ProtocolEnvelope`, codec JSON y TCP.
- `auth-as`: validacion inicial del cliente.
- `auth-tgs`: emision conceptual de acceso al servicio.
- `auth-service`: validacion final y recurso protegido.
- `auth-client-sdk`: `AuthClient`, `AuthFlowRunner`, CLI y auditorias.
- `auth-storage-sqlite`: repositorios SQLite, migraciones y CLI local.
- `auth-storage-postgres`: repositorios PostgreSQL/RDS-ready.
- `auth-websocket-gateway`: frontera de integracion WebSocket.
- `auth-web-demo`: visualizacion tecnica del protocolo.
- `sample-login-app`: app de ejemplo para integradores.

## Docker

`docker-compose.yml` define servicios para AS, TGS, Service, Gateway, frontends
y PostgreSQL opcional con perfil `postgres-local`.

Dockerfiles:

- `docker/as/Dockerfile`: empaqueta runtime de AS.
- `docker/tgs/Dockerfile`: empaqueta runtime de TGS.
- `docker/service/Dockerfile`: empaqueta runtime de Service.
- `docker/gateway/Dockerfile`: empaqueta Gateway y dependencias runtime.
- `docker/web-demo/Dockerfile`: sirve `auth-web-demo`.
- `docker/sample-login-app/Dockerfile`: sirve `sample-login-app`.

## Terraform

- `infra/aws/terraform/main.tf`: VPC, ALB, ECS/Fargate, ECR, RDS, logs y rutas.
- `variables.tf`: parametros del blueprint.
- `outputs.tf`: salidas utiles del plan.
- `terraform.tfvars.example`: variables ejemplo HTTP/local blueprint.
- `terraform.tfvars.https.example`: variables ejemplo HTTPS/WSS.

No se usa RSA/JWT en esta fase porque el objetivo es mantener sesiones opacas
server-side y no exponer tokens auto-verificables al frontend.

PostgreSQL es la ruta preparada para multiples instancias del Gateway porque
`VERIFY_SESSION` y `LOGOUT_SESSION` deben consultar estado compartido. Memoria
local no sirve para replicas cloud.

WSS debe terminar en ALB/ACM para que el navegador use TLS y el Gateway reciba
trafico interno controlado.

## Mensajes Conceptuales En UI

La UI muestra nombres y contenido conceptual:

- `START_AUTH_FLOW`: clientId, serviceId, requestId.
- AS request/response: identidad, TGS solicitado, lifetime conceptual.
- TGS request/response: ticket conceptual, autenticador, servicio solicitado.
- Service request/response: decision de acceso y respuesta protegida.
- `FLOW_RESULT`: exito/fallo, sesion opaca, expiracion y latencia.
- `VERIFY_SESSION`: sesion opaca y contexto cliente/servicio.

No muestra material criptografico real.

## Amenazas Y Limites

Amenazas visualizadas:

- cliente desconocido;
- servicio desconocido;
- sesion invalida;
- replay protection;
- sesion expirada;
- exposicion de datos sensibles.

Validaciones automatizadas relevantes:

- codec JSON;
- AES-GCM;
- flujo AS -> TGS -> Service;
- replay cache;
- sesiones opacas;
- Gateway WebSocket;
- SQLite/PostgreSQL.

Limites abiertos:

- no se ejecuto AWS apply;
- no se hizo hardening productivo completo;
- WSS requiere ACM real;
- secretos cloud deben vivir en Secrets Manager;
- las demos son locales y no reemplazan pruebas automatizadas.
