# Architecture

El proyecto mantiene dos rutas ejecutables:

- **Legacy**: demo historica en `Kerberos/` y `Seguridad/`.
- **Modular**: runtime nuevo en `auth-as`, `auth-tgs`, `auth-service` y
  `auth-client-sdk`.

No es MIT Kerberos oficial y no debe presentarse como listo para produccion
critica.

## Ruta Legacy

| Clase | Responsabilidad |
| --- | --- |
| `Kerberos.AuthenticationServer` | Recibe solicitud inicial y emite Ticket TGS |
| `Kerberos.TicketGrantingServer` | Valida Ticket TGS y emite ticket de servicio |
| `Kerberos.ServiceServer` | Valida ticket de servicio y autenticador |
| `Kerberos.Client` | Ejecuta el flujo completo como cliente |
| `Kerberos.AESUtils` | Cifrado legacy basado en `SealedObject` |
| `Seguridad.Comunicacion` | `ObjectInputStream` / `ObjectOutputStream` |

Esta ruta sigue disponible para comparar comportamiento y para conservar la demo
original. No se elimino ni se migro a AES-GCM.

## Ruta Modular

| Modulo | Responsabilidad | Estado |
| --- | --- | --- |
| `auth-core` | DTOs, `AuthConfig`, `ReplayCache` | Activo |
| `auth-crypto` | `CryptoEnvelope`, AES-GCM, derivacion de claves | Activo |
| `auth-transport` | `ProtocolEnvelope`, JSON, TCP, DTOs seguros, mappers legacy | Activo |
| `auth-as` | `AuthenticationServerApp`, `AuthenticationHandler` | Ejecutable |
| `auth-tgs` | `TicketGrantingServerApp`, `TicketGrantingHandler` | Ejecutable |
| `auth-service` | `ProtectedServiceApp`, `ProtectedServiceHandler` | Ejecutable |
| `auth-client-sdk` | `AuthClient`, `AuthFlowRunner`, `ClientCli` | Ejecutable |

La ruta modular no usa `Kerberos.AuthenticationServer`,
`Kerberos.TicketGrantingServer`, `Kerberos.ServiceServer`, `Kerberos.Client`,
`AESUtils` ni `SealedObject`.

## Transporte

La ruta modular usa:

- `ProtocolEnvelope` con `messageType`, `version`, `requestId`, `issuedAt` y
  `payload`.
- `JsonMessageCodec`, un codec JSON acotado a los DTOs del proyecto.
- `TcpMessageClient` y `TcpMessageServer`, con un mensaje JSON por conexion.

La comunicacion legacy con serializacion Java sigue existiendo solo para la
demo historica.

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

`AESUtils` permanece sin cambios para que la demo legacy siga funcionando.

## Pruebas

La suite Maven incluye pruebas para:

- replay cache;
- configuracion;
- AES-GCM;
- codec JSON;
- cifrado JSON + AES-GCM;
- mappers legacy;
- integracion modular AS -> TGS -> Service;
- replay, servicio inexistente, autenticador invalido y requestId repetido.

En esta sesion `mvn` no estuvo disponible en PATH, por lo que se compilaron
fuentes y tests con `javac` como verificacion complementaria y se ejecuto un
smoke modular real.

## Pendiente

- Ejecutar `mvn test` en un entorno con Maven disponible.
- Sustituir secretos demo por configuracion local no versionada.
- Evaluar un JSON parser mantenido si se autoriza una dependencia externa.
- Agregar Docker, WebSockets o frontend solo en fases futuras autorizadas.
