# Architecture

La arquitectura actual esta centrada en la ruta modular `auth-*`. El codigo
legacy fisico ya fue retirado, y Fase 9 elimino tambien los paquetes internos
`auth-transport/javaio` y `auth-transport/legacy`. Fase 10 agrego
`auth-websocket-gateway` como capa separada de integracion. Fase 12 + Fase 13
agregan `auth-web-demo`, una demo web local que consume el gateway sin acoplarse
al runtime TCP modular. Fase 14 agrega pruebas formales de concurrencia y
`auth-storage-sqlite` como primera integracion persistente local. Fase 15 agrega
migraciones SQLite, auditoria persistente, administracion local y
`sample-login-app`. Fase 16 endurece configuracion, Gateway y auditoria
consultable; Fase 17 agrega Docker Compose local sin reemplazar la ejecucion sin
Docker. Fase 18 agrega sesiones opacas verificables en el Gateway. Fase 19
mueve el proyecto real a la raiz y prepara validacion Docker Linux y AWS sin
desplegar infraestructura.

No es MIT Kerberos oficial y no debe presentarse como listo para produccion
critica.

## Modulos

| Modulo | Responsabilidad | Estado |
| --- | --- | --- |
| `auth-core` | DTOs, `AuthConfig`, `ReplayCache` | Activo |
| `auth-crypto` | `CryptoEnvelope`, AES-GCM, derivacion de claves | Activo |
| `auth-transport` | `ProtocolEnvelope`, JSON, TCP y DTOs seguros | Activo |
| `auth-storage-sqlite` | Repositorios SQLite, migraciones, auditoria y CLI local | Activo |
| `auth-as` | `AuthenticationServerApp`, `AuthenticationHandler` | Ejecutable |
| `auth-tgs` | `TicketGrantingServerApp`, `TicketGrantingHandler` | Ejecutable |
| `auth-service` | `ProtectedServiceApp`, `ProtectedServiceHandler` | Ejecutable |
| `auth-client-sdk` | `AuthClient`, `AuthFlowRunner`, `ClientCli`, audit runner | Ejecutable |
| `auth-websocket-gateway` | WebSocket Gateway separado sobre `AuthClient` | Ejecutable |
| `auth-web-demo` | Frontend vanilla local que consume el Gateway WebSocket | Demo local |
| `sample-login-app` | Mini app vanilla tipo login para integradores | Demo local |
| `docs` | Documentacion y auditorias | Activo |

## Flujo Principal

1. Client solicita al AS un ticket TGS.
2. AS devuelve una respuesta cifrada para el cliente y un ticket cifrado para
   TGS.
3. Client presenta ticket y autenticador al TGS.
4. TGS devuelve ticket de servicio y clave de sesion cliente-servicio.
5. Client presenta ticket y autenticador al Service.
6. Service devuelve `ServiceResponse` cifrado.

## Storage

La ruta modular soporta dos modos:

- `AUTH_STORAGE_MODE=memory`: modo demo por defecto con repositorios en memoria.
- `AUTH_STORAGE_MODE=sqlite`: AS, TGS y Service cargan clientes, TGS y servicios
  desde una base SQLite local indicada por `AUTH_SQLITE_PATH`.

Las interfaces viven en `auth-core`:

- `PrincipalRepository`
- `ServiceRegistry`

Implementaciones actuales:

- `InMemoryPrincipalRepository`
- `InMemoryServiceRegistry`
- `SQLitePrincipalRepository`
- `SQLiteServiceRegistry`

SQLite se mantiene en un modulo separado para evitar acoplar `auth-core` a JDBC.
No hay ORM ni servidor externo.

Fase 15 agrega migraciones versionadas en `scripts/sqlite/migrations/` y una
tabla `schema_version`. `SQLiteDemoDatabaseInitializer` aplica migraciones en
vez de depender solo de scripts sueltos.

## Auditoria Persistente

`auth-core` define `AuditRepository` y `AuthEventRepository`. En modo memoria se
usa `NoOpAuthEventRepository`. En modo SQLite, `auth-websocket-gateway` usa
`SQLiteAuditRepository` para registrar inicio, exito y fallo del flujo.

La auditoria persiste `requestId`, `clientId`, `serviceId`, `eventType`,
`status`, `errorType`, `latencyMs` y `createdAt`. No registra secretos, tickets
completos, claves, ciphertexts ni payloads internos.

## Sesiones Opacas

`auth-core` define `AuthSession`, `SessionRepository` e
`InMemorySessionRepository`. `auth-storage-sqlite` implementa
`SQLiteSessionRepository` sobre `auth_sessions`.

El Gateway crea una sesion solo si AS -> TGS -> Service termina con acceso
concedido. La app externa debe verificarla con `VERIFY_SESSION`; no debe abrir
recursos solo por `FLOW_RESULT.success=true`.

## API De Servicio Protegido

`auth-service` expone `ProtectedResource` como interfaz:

- `getServiceId()`
- `execute(ProtectedServiceRequest request)`

`ProtectedServiceHandler` valida el protocolo antes de llamar al recurso. La
implementacion actual es `DemoProtectedResource`; servicios reales pueden crear
su propia implementacion sin cambiar AS/TGS. `HttpProtectedResource` sirve como
ejemplo de adaptador HTTP local simple para integraciones externas.

## WebSocket Gateway

`auth-websocket-gateway` agrega una capa de integracion para clientes WebSocket.
No modifica ni reemplaza `auth-as`, `auth-tgs` ni `auth-service`; usa
`AuthClient` para ejecutar el flujo AS -> TGS -> Service sobre la ruta TCP
modular.

Mensajes de entrada soportados:

- `START_AUTH_FLOW`
- `VERIFY_SESSION`
- `LOGOUT_SESSION`
- `PING`

Mensajes de salida:

- `FLOW_EVENT`
- `FLOW_RESULT`
- `SESSION_VALID`
- `SESSION_INVALID`
- `SESSION_LOGGED_OUT`
- `ERROR`
- `PONG`

El gateway emite eventos como `AS_REQUEST_SENT`, `TGS_RESPONSE_RECEIVED`,
`SERVICE_RESPONSE_RECEIVED` y `FLOW_SUCCESS`, junto con latencias basicas por
etapa.

## Frontend Demo

`auth-web-demo` es una aplicacion estatica en HTML, CSS y JavaScript vanilla. Se
sirve localmente con un script Node propio, no usa bundler ni framework, y solo
habla con `auth-websocket-gateway` mediante el contrato documentado en
`docs/frontend-contract.md`.

La UI muestra:

- estado de conexion WebSocket;
- etapas Client, Gateway, AS, TGS y Service;
- eventos `FLOW_*`;
- `FLOW_RESULT`, latencias y errores controlados.
- sesion opaca enmascarada, expiracion y estado de verificacion.

No muestra claves, secretos, tickets completos, ciphertexts ni payloads
criptograficos.

`sample-login-app` tambien es vanilla y no usa npm. A diferencia de
`auth-web-demo`, simula una app real con pantalla de login y zona protegida. El
dashboard se muestra solo despues de `SESSION_VALID`.

## Transporte

La ruta modular usa `ProtocolEnvelope`, `JsonMessageCodec`, `TcpMessageClient`
y `TcpMessageServer`. El transporte tiene timeout de conexion, timeout de
lectura, limite de tamano de mensaje, cierre de sockets y validacion opcional
del `MessageType` esperado.

## Criptografia

La ruta modular usa `AesGcmCryptoService`, `CryptoEnvelope`,
`AesKeyDerivation`, `SessionKeys` y associated data estable en `SecureAad`.

Se cifran con AES-GCM:

- `TicketTgs`
- `TicketService`
- `ClientAuthenticator`
- `SecureAsResponse`
- `SecureTgsResponse`
- `ServiceResponse`

## Configuracion

`AuthConfig` soporta:

- `AUTH_MODE=demo`: permite secretos por defecto para demo y muestra advertencia.
- `AUTH_MODE=strict`: exige secretos explicitos y rechaza defaults.
- `AUTH_STORAGE_MODE=memory|sqlite`: selecciona repositorios en memoria o
  SQLite local.
- `AUTH_SQLITE_PATH`: ruta de base SQLite cuando se usa `sqlite`.
- `AUTH_SESSION_TTL_SECONDS` y `AUTH_SESSION_MAX_TTL_SECONDS`.
- `AUTH_SESSION_STORAGE_MODE=memory|sqlite`.
- `AUTH_REQUIRE_SESSION_VERIFY`.

Los nombres principales de secretos son `AUTH_DEMO_*`. En `AUTH_MODE=strict`,
`AuthConfig` exige valores explicitos y rechaza los defaults de demo.

## Pruebas Y CI

La suite Maven cubre replay cache, configuracion, AES-GCM, codec JSON,
transporte seguro JSON + AES-GCM, integracion modular con casos negativos,
concurrencia, SQLite local, migraciones, auditoria persistente, administracion
SQLite, `ProtectedResource` HTTP local, pruebas unitarias del WebSocket Gateway y
una prueba E2E WebSocket real.

La demo web se valida por separado con `npm install` y `npm run build` dentro de
`auth-web-demo`.

GitHub Actions vive en la raiz del repositorio Git en
`.github/workflows/maven.yml` y ejecuta desde la raiz del repositorio:

- `mvn -q -DskipTests compile`
- `mvn test`
- `mvn -pl auth-websocket-gateway -am test`
- `mvn -pl auth-storage-sqlite -am test`

## Pendiente

- Evaluar un JSON parser mantenido si el codec propio crece fuera de su alcance
  acotado.
- Endurecer secretos SQLite, transporte y auditoria si la integracion crece.
- Validar Docker Compose en Linux con `docker compose config/build/up`.
- Evaluar pruebas E2E de navegador para la demo web.
