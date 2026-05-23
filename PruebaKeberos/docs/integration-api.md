# Integration API

Este documento resume como conectar aplicaciones reales al runtime modular sin
acoplarlas a la base SQLite interna.

SQLite pertenece al sistema de autenticacion. Una app externa no debe abrirla ni
consultarla directamente; debe usar `auth-websocket-gateway` o `auth-client-sdk`
en integraciones Java controladas.

## Responsibilities

El sistema de autenticacion:

- ejecuta AS -> TGS -> Service;
- valida tickets, autenticadores, expiracion, clock skew y replay local;
- emite sesiones opacas verificables en el Gateway;
- registra auditoria SQLite cuando corre con `AUTH_STORAGE_MODE=sqlite`.

La app integradora:

- maneja UI y experiencia de usuario;
- llama `START_AUTH_FLOW`;
- llama `VERIFY_SESSION` antes de abrir recursos;
- llama `LOGOUT_SESSION` al cerrar sesion;
- mantiene roles, permisos y autorizacion de negocio;
- usa TLS/WSS en entornos no locales.

## WebSocket Contract

Entrada:

- `START_AUTH_FLOW`
- `VERIFY_SESSION`
- `LOGOUT_SESSION`
- `PING`

Salida:

- `FLOW_EVENT`
- `FLOW_RESULT`
- `SESSION_VALID`
- `SESSION_INVALID`
- `SESSION_LOGGED_OUT`
- `ERROR`
- `PONG`

`FLOW_RESULT.success=true` no es autorizacion final. Solo indica que el Gateway
pudo crear una sesion server-side despues de un flujo exitoso.

## How To Secure Your Own App With This Project

1. Registrar cliente con `scripts/sqlite-admin.sh ... clients add`.
2. Registrar servicio con `scripts/sqlite-admin.sh ... services add`.
3. Configurar `AUTH_STORAGE_MODE=sqlite` y `AUTH_SQLITE_PATH`.
4. Levantar AS, TGS, Service y WebSocket Gateway.
5. Enviar `START_AUTH_FLOW`.
6. Si `FLOW_RESULT.success === true`, guardar `sessionId` solo en memoria.
7. Enviar `VERIFY_SESSION` con `sessionId`, `clientId` y `serviceId`.
8. Conceder acceso solo despues de `SESSION_VALID`.
9. En logout, enviar `LOGOUT_SESSION`.
10. Consultar auditoria con `audit list`, `by-request`, `by-client` o
    `by-service`.
11. Documentar limitaciones: sin TLS/mTLS integrado, sin vault externo, sin
    garantias de produccion critica.

## Protected Resource

`auth-service` define:

```java
String getServiceId();
ProtectedServiceResponse execute(ProtectedServiceRequest request);
```

`ProtectedServiceHandler` valida el protocolo antes de invocar el recurso. Un
servicio real puede vivir detras de `auth-service`; no necesita conectarse a
SQLite directamente.

Implementaciones actuales:

- `DemoProtectedResource`: respuesta local de demostracion.
- `HttpProtectedResource`: ejemplo contra un servidor HTTP local en pruebas.

## Notes For Cloud

`ws://` es solo para desarrollo local. En cloud se debe publicar `wss://` con
terminacion TLS, limites operativos, logs sin secretos y gestion real de
secretos. RSA/JWT/CA quedan como posibles evoluciones, no como requisito de esta
fase.
