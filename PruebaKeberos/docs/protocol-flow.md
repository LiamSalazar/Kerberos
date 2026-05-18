# Protocol Flow

Este documento describe la ruta modular actual y separa explicitamente la ruta
legacy.

## Ruta Modular

Todos los mensajes modulares viajan como JSON dentro de `ProtocolEnvelope`:

- `messageType`
- `version`
- `requestId`
- `issuedAt`
- `payload`

El payload puede ser un DTO en claro o un `CryptoEnvelope` con ciphertext
AES-GCM.

## 1. Client -> AS

Mensaje: `AS_REQUEST`

Payload: `AsRequest`

Contenido principal:

- `clientId`
- `ticketGrantingServerId`
- `requestId`
- `issuedAt`

Estado: modular y JSON. No usa serializacion Java.

## 2. AS -> Client

Mensaje: `AS_RESPONSE`

Payload externo: `CryptoEnvelope`

Plaintext cifrado para el cliente: `SecureAsResponse`

`SecureAsResponse` contiene:

- clave de sesion cliente-TGS;
- metadata de expiracion;
- `ticketTgsEnvelope`.

`ticketTgsEnvelope` cifra un `TicketTgs` con la clave del TGS.

Estado: AES-GCM real. No usa `AESUtils` ni `SealedObject`.

## 3. Client -> TGS

Mensaje: `TGS_REQUEST`

Payload: `SecureTgsRequest`

Contenido principal:

- `serviceId`
- `ticketTgsEnvelope`
- `clientAuthenticatorEnvelope`

El autenticador es un `ClientAuthenticator` cifrado con la clave de sesion
cliente-TGS.

Estado: JSON modular. El TGS descifra ticket y autenticador con AES-GCM.

## 4. TGS -> Client

Mensaje: `TGS_RESPONSE`

Payload externo: `CryptoEnvelope`

Plaintext cifrado para el cliente: `SecureTgsResponse`

`SecureTgsResponse` contiene:

- clave de sesion cliente-servicio;
- `ticketServiceEnvelope`.

`ticketServiceEnvelope` cifra un `TicketService` con la clave del servicio.

## 5. Client -> Service

Mensaje: `SERVICE_REQUEST`

Payload: `SecureServiceRequest`

Contenido principal:

- `ticketServiceEnvelope`
- `clientAuthenticatorEnvelope`

El autenticador es un `ClientAuthenticator` cifrado con la clave de sesion
cliente-servicio.

## 6. Service -> Client

Mensaje: `SERVICE_RESPONSE`

Payload externo: `CryptoEnvelope`

Plaintext cifrado para el cliente: `ServiceResponse`

El cliente descifra la respuesta con la clave de sesion cliente-servicio.

## Replay Y Errores

TGS y Service usan `InMemoryReplayCache` para bloquear:

- `requestId` repetidos;
- autenticadores reutilizados.

Los errores vuelven como `ERROR_RESPONSE` con payload `ErrorResponse`.

## Ruta Legacy

La ruta legacy mantiene los payloads historicos:

- `HashMap<String,Object>`
- `SealedObject`
- `AESUtils`
- `ObjectInputStream` / `ObjectOutputStream`

Sus mappers (`Legacy*Mapper`) siguen disponibles para compatibilidad, pero no
son el contrato principal del runtime modular.

## Pendiente

- Ejecutar la suite Maven completa en entorno con Maven.
- Agregar mas casos de expiracion de tickets.
- Evaluar reemplazo del codec JSON manual por una biblioteca si se autoriza una
  dependencia.
