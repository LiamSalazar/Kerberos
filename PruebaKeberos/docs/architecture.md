# Architecture

La arquitectura actual esta centrada en la ruta modular `auth-*`. Las carpetas
legacy historicas fueron retiradas fisicamente en Fase 8.1 despues de validar
compilacion, pruebas Maven y ausencia de imports desde `auth-*` hacia los
paquetes historicos.

No es MIT Kerberos oficial y no debe presentarse como listo para produccion
critica.

## Modulos

| Modulo | Responsabilidad | Estado |
| --- | --- | --- |
| `auth-core` | DTOs, `AuthConfig`, `ReplayCache` | Activo |
| `auth-crypto` | `CryptoEnvelope`, AES-GCM, derivacion de claves | Activo |
| `auth-transport` | `ProtocolEnvelope`, JSON, TCP y DTOs seguros | Activo |
| `auth-as` | `AuthenticationServerApp`, `AuthenticationHandler` | Ejecutable |
| `auth-tgs` | `TicketGrantingServerApp`, `TicketGrantingHandler` | Ejecutable |
| `auth-service` | `ProtectedServiceApp`, `ProtectedServiceHandler` | Ejecutable |
| `auth-client-sdk` | `AuthClient`, `AuthFlowRunner`, `ClientCli`, audit runner | Ejecutable |
| `auth-websocket-gateway` | WebSocket Gateway separado sobre `AuthClient` | Ejecutable |
| `docs` | Documentacion y auditorias | Activo |

## Flujo Principal

1. Client solicita al AS un ticket TGS.
2. AS devuelve una respuesta cifrada para el cliente y un ticket cifrado para
   TGS.
3. Client presenta ticket y autenticador al TGS.
4. TGS devuelve ticket de servicio y clave de sesion cliente-servicio.
5. Client presenta ticket y autenticador al Service.
6. Service devuelve `ServiceResponse` cifrado.

## WebSocket Gateway

`auth-websocket-gateway` agrega una capa de integracion para una futura interfaz
web. No modifica ni reemplaza `auth-as`, `auth-tgs` ni `auth-service`; usa
`AuthClient` para ejecutar el flujo AS -> TGS -> Service sobre la ruta TCP
modular.

Mensajes de entrada soportados:

- `START_AUTH_FLOW`
- `RUN_AUDIT_FLOW`
- `PING`

Mensajes de salida:

- `GATEWAY_READY`
- `FLOW_EVENT`
- `FLOW_RESULT`
- `ERROR`
- `PONG`

El gateway emite eventos como `AS_REQUEST_SENT`, `TGS_RESPONSE_RECEIVED`,
`SERVICE_RESPONSE_RECEIVED` y `FLOW_SUCCESS`, junto con latencias basicas por
etapa.

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

- `AUTH_MODE=demo` o `AUTH_MODE=local`: permite secretos por defecto para demo.
- `AUTH_MODE=strict`: exige secretos explicitos y rechaza defaults.

Los nombres principales de secretos son `AUTH_DEMO_*`. Los nombres
`AUTH_LEGACY_*` se aceptan solo como alias temporal de compatibilidad en
`AuthConfig`.

## Pruebas Y CI

La suite Maven cubre replay cache, configuracion, AES-GCM, codec JSON,
transporte seguro JSON + AES-GCM, integracion modular con casos negativos y
pruebas unitarias del WebSocket Gateway.

GitHub Actions vive en la raiz del repositorio Git en
`../.github/workflows/maven.yml` y ejecuta desde `PruebaKeberos`:

- `mvn -q -DskipTests compile`
- `mvn test`

## Pendiente

- Retirar aliases `AUTH_LEGACY_*` cuando deje de ser necesaria la compatibilidad.
- Evaluar un JSON parser mantenido si el codec propio crece fuera de su alcance
  acotado.
- Agregar pruebas WebSocket end-to-end con servicios reales si se decide
  ampliar la suite de integracion.
- Agregar Docker o frontend solo en fases futuras autorizadas.
