# Integration API

Este documento resume como conectar aplicaciones y servicios reales al runtime
modular sin reemplazar la arquitectura `auth-*`.

## Responsabilidades

Este sistema asume:

- ejecutar el flujo AS -> TGS -> Service;
- validar tickets, autenticadores, expiracion, clock skew y replay local;
- devolver `ServiceResponse` al cliente modular;
- exponer `FLOW_RESULT` por WebSocket Gateway;
- registrar auditoria local SQLite cuando el Gateway corre en modo SQLite.

La aplicacion integradora conserva:

- UI y experiencia de usuario;
- sesion web propia;
- autorizacion de negocio;
- TLS y proteccion de transporte;
- almacenamiento de usuarios si aplica;
- manejo de logout y expiracion de sesion.

## Repositorios De Identidad

`auth-core` define `PrincipalRepository`:

```java
Optional<String> clientSecret(String clientId);
Optional<String> ticketGrantingServerSecret(String tgsId);
```

Implementaciones actuales:

- `InMemoryPrincipalRepository`
- `SQLitePrincipalRepository`

## Registro De Servicios

`auth-core` define `ServiceRegistry`:

```java
Optional<String> ticketGrantingServerSecret(String tgsId);
Optional<String> serviceSecret(String serviceId);
```

Implementaciones actuales:

- `InMemoryServiceRegistry`
- `SQLiteServiceRegistry`

## Auditoria

`auth-core` define:

```java
void append(AuthAuditEvent event);
List<AuthAuditEvent> findRecent(int limit);
List<AuthAuditEvent> findByRequestId(String requestId);
```

Implementaciones actuales:

- `NoOpAuthEventRepository` para `AUTH_STORAGE_MODE=memory`;
- `SQLiteAuditRepository` para `AUTH_STORAGE_MODE=sqlite`.

Eventos persistidos:

- `AUTH_FLOW_STARTED`
- `AUTH_FLOW_SUCCEEDED`
- `AUTH_FLOW_FAILED`

## Recurso Protegido

`auth-service` define:

```java
String getServiceId();
ProtectedServiceResponse execute(ProtectedServiceRequest request);
```

`ProtectedServiceHandler` valida el protocolo antes de invocar el recurso.

Implementaciones actuales:

- `DemoProtectedResource`: respuesta local de demostracion.
- `HttpProtectedResource`: ejemplo que invoca un servidor HTTP local simple en
  pruebas. No consume APIs externas reales.

## Registrar Cliente Y Servicio

Inicializar base:

```cmd
scripts\init-sqlite-demo.bat --db data\auth-demo.sqlite
```

Registrar cliente:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients add --id app-client --display-name "App Client" --secret "<secret>"
```

Registrar servicio:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services add --id melodyfinder --display-name "MelodyFinder" --secret "<secret>" --endpoint local://melodyfinder
```

## Conectar SQLite

```cmd
set AUTH_STORAGE_MODE=sqlite
set AUTH_SQLITE_PATH=data\auth-demo.sqlite
```

Usar las mismas variables al levantar AS, TGS, Service y Gateway.

## Conectar Una App Externa Por WebSocket

1. Abrir `ws://127.0.0.1:2800`.
2. Esperar `GATEWAY_READY`.
3. Enviar:

```json
{"type":"START_AUTH_FLOW","requestId":"app-req-1","clientId":"app-client","serviceId":"melodyfinder"}
```

4. Procesar `FLOW_EVENT` como progreso.
5. Conceder acceso solo si `FLOW_RESULT.success` es `true`.
6. Mostrar `requestId` y mensaje de servicio de alto nivel.
7. Consultar auditoria con `sqlite-admin ... audit list` si se usa SQLite.

## Mini App De Referencia

`sample-login-app/` demuestra el patron anterior con HTML/CSS/JS vanilla. Ver:

- `docs/sample-login-app.md`
- `sample-login-app/index.html`
- `sample-login-app/app.js`

## Limites Actuales

- No hay API HTTP de administracion.
- No hay vault externo.
- No hay TLS/mTLS.
- No hay autorizacion granular por rol o scope.
- No es production-ready.
