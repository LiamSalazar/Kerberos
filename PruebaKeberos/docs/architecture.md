# Architecture

El proyecto esta centrado en la ruta modular. La demo legacy en `Kerberos/` y
`Seguridad/` sigue en el repositorio como referencia historica porque en esta
fase no se pudo ejecutar Maven, y la eliminacion controlada exige que
`mvn -q -DskipTests compile` y `mvn test` pasen antes de borrar codigo.

No es MIT Kerberos oficial y no debe presentarse como listo para produccion
critica.

## Rutas

| Ruta | Estado | Uso actual |
| --- | --- | --- |
| Modular `auth-*` | Principal | DTOs, JSON/TCP, AES-GCM, replay cache, pruebas y auditoria |
| Legacy `Kerberos/` + `Seguridad/` | Conservada | Referencia historica; no es runtime principal |

La auditoria `docs/audits/legacy-dependency-audit.md` no encontro imports desde
`auth-*` hacia `Kerberos.*` o `Seguridad.*`.

## Modulos Modulares

| Modulo | Responsabilidad | Estado |
| --- | --- | --- |
| `auth-core` | DTOs, `AuthConfig`, `ReplayCache` | Activo |
| `auth-crypto` | `CryptoEnvelope`, AES-GCM, derivacion de claves | Activo |
| `auth-transport` | `ProtocolEnvelope`, JSON, TCP, DTOs seguros, mappers legacy aislados | Activo |
| `auth-as` | `AuthenticationServerApp`, `AuthenticationHandler` | Ejecutable |
| `auth-tgs` | `TicketGrantingServerApp`, `TicketGrantingHandler` | Ejecutable |
| `auth-service` | `ProtectedServiceApp`, `ProtectedServiceHandler` | Ejecutable |
| `auth-client-sdk` | `AuthClient`, `AuthFlowRunner`, `ClientCli`, audit runner | Ejecutable |

La ruta modular no usa `Kerberos.AuthenticationServer`,
`Kerberos.TicketGrantingServer`, `Kerberos.ServiceServer`, `Kerberos.Client`,
`AESUtils` ni `SealedObject`.

## Transporte

La ruta modular usa:

- `ProtocolEnvelope` con `messageType`, `version`, `requestId`, `issuedAt` y
  `payload`.
- `JsonMessageCodec`, un codec JSON acotado a los DTOs del proyecto.
- `TcpMessageClient` con timeout de conexion, timeout de lectura y limite de
  tamano de mensaje.
- `TcpMessageServer` con timeout de lectura, limite de tamano de mensaje,
  cierre de sockets y validacion opcional del `MessageType` esperado.

`auth-transport/javaio` queda como adaptador de compatibilidad, no como ruta
modular.

## Criptografia

La ruta modular usa:

- `AesGcmCryptoService`
- `CryptoEnvelope`
- `AesKeyDerivation`
- `SessionKeys`
- associated data estable en `SecureAad`

Se cifran con AES-GCM:

- `TicketTgs`
- `TicketService`
- `ClientAuthenticator`
- `SecureAsResponse`
- `SecureTgsResponse`
- `ServiceResponse`

`AESUtils` permanece solo en legacy.

## Configuracion

`AuthConfig` soporta:

- `AUTH_MODE=demo` o `AUTH_MODE=local`: permite secretos por defecto para demo.
- `AUTH_MODE=strict`: exige secretos explicitos y rechaza defaults.

Los nombres de variables siguen usando prefijo `AUTH_LEGACY_*` por compatibilidad
historica de configuracion, aunque la ruta modular use AES-GCM.

## Pruebas Y Auditoria

La suite Maven esperada cubre replay cache, configuracion, AES-GCM, codec JSON,
mappers legacy, transporte seguro JSON + AES-GCM e integracion modular con casos
negativos.

En esta sesion `mvn` no estuvo disponible en PATH. Como verificacion auxiliar:

- fuentes principales compilaron con `javac`;
- fuentes de test compilaron con `javac`;
- `ModularAuthAuditRunner` ejecuto 3 flujos completos en puertos
  `2500/2501/2502` con 3 exitos y 0 fallos.

La evidencia esta en `docs/audits/latest-run.md` y
`docs/audits/latest-run.json`.

## Pendiente

- Ejecutar `mvn test` en un entorno con Maven disponible.
- Si Maven pasa, eliminar `Kerberos/` y `Seguridad/` del runtime principal.
- Evaluar un JSON parser mantenido si se autoriza una dependencia externa.
- Agregar Docker, WebSockets o frontend solo en fases futuras autorizadas.
