# Integration API

Este documento resume como conectar aplicaciones y servicios reales al runtime
modular sin reemplazar la arquitectura `auth-*`.

SQLite es persistencia interna del sistema de autenticacion. Una app externa no
debe conectarse directamente a SQLite; debe hablar con `auth-websocket-gateway`
o usar `auth-client-sdk` si es una integracion Java controlada.

## Responsabilidades

Este sistema asume:

- ejecutar el flujo AS -> TGS -> Service;
- validar tickets, autenticadores, expiracion, clock skew y replay local;
- devolver `FLOW_RESULT` por WebSocket Gateway;
- registrar auditoria SQLite cuando el Gateway corre con `AUTH_STORAGE_MODE=sqlite`.

La aplicacion integradora conserva:

- UI y experiencia de usuario;
- sesion web propia;
- autorizacion de negocio;
- TLS y proteccion de transporte;
- almacenamiento de usuarios si aplica;
- manejo de logout y expiracion de sesion.

## Contrato WebSocket

Entrada:

- `START_AUTH_FLOW`
- `PING`

Salida:

- `FLOW_EVENT`
- `FLOW_RESULT`
- `ERROR`
- `PONG`

`START_AUTH_FLOW` requiere `type`, `requestId`, `clientId` y `serviceId`:

```json
{"type":"START_AUTH_FLOW","requestId":"app-req-1","clientId":"app-client","serviceId":"melodyfinder"}
```

Errores tipados:

- `INVALID_JSON`
- `UNKNOWN_MESSAGE_TYPE`
- `MISSING_REQUIRED_FIELD`
- `CLIENT_NOT_FOUND`
- `SERVICE_NOT_FOUND`
- `FLOW_FAILED`
- `RATE_LIMITED`
- `ORIGIN_NOT_ALLOWED`

`AUTH_ALLOWED_ORIGINS` acepta una lista separada por comas. Si esta vacia, el
Gateway permite origenes para ejecucion local. Si se configura, el `Origin` del
cliente WebSocket debe coincidir exactamente.

## Auditoria

`auth-core` define:

```java
void append(AuthAuditEvent event);
List<AuthAuditEvent> findRecent(int limit);
List<AuthAuditEvent> findByRequestId(String requestId);
List<AuthAuditEvent> findByClientId(String clientId);
List<AuthAuditEvent> findByServiceId(String serviceId);
```

Eventos persistidos:

- `requestId`
- `clientId`
- `serviceId`
- `eventType`
- `status`
- `errorType`
- `latencyMs`
- `createdAt`

CLI:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit list --limit 20
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-request --request-id app-req-1
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-client --client-id app-client
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-service --service-id melodyfinder
```

La salida no imprime secretos, tickets, claves ni ciphertexts.

## Recurso Protegido

`auth-service` define:

```java
String getServiceId();
ProtectedServiceResponse execute(ProtectedServiceRequest request);
```

`ProtectedServiceHandler` valida el protocolo antes de invocar el recurso. Un
servicio real puede ubicarse detras de `auth-service`; no necesita consultar
SQLite directamente.

Implementaciones actuales:

- `DemoProtectedResource`: respuesta local de demostracion.
- `HttpProtectedResource`: ejemplo que invoca un servidor HTTP local simple en
  pruebas. No consume APIs externas reales.

## How to secure your own app with this project

1. Registrar cliente con `sqlite-admin ... clients add`.
2. Registrar servicio con `sqlite-admin ... services add`.
3. Configurar `AUTH_STORAGE_MODE=sqlite` y `AUTH_SQLITE_PATH`.
4. Levantar AS, TGS, Service y WebSocket Gateway.
5. Enviar `START_AUTH_FLOW`.
6. Validar `FLOW_RESULT.success === true`.
7. Conceder acceso al recurso propio.
8. Consultar auditoria con `audit list`, `by-request`, `by-client` o `by-service`.
9. Documentar limitaciones: sin TLS/mTLS integrado, sin vault externo y sin
   garantias de produccion critica.

Ver tambien [using-auth-api.md](using-auth-api.md) y
[sample-login-app.md](sample-login-app.md).
