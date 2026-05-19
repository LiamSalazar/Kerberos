# Protocol Flow

Este documento describe la ruta modular principal y separa explicitamente la
ruta legacy conservada.

## Envelope Modular

Todos los mensajes modulares viajan como JSON dentro de `ProtocolEnvelope`:

- `messageType`
- `version`
- `requestId`
- `issuedAt`
- `payload`

El transporte TCP usa un mensaje JSON por conexion. El servidor modular valida
payload no vacio, tamano maximo, timeout de lectura y, cuando se configura,
`MessageType` esperado por endpoint.

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

`ticketTgsEnvelope` cifra un `TicketTgs` con el secreto del TGS.

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

El TGS valida:

- `requestId` no repetido;
- servicio registrado;
- ticket TGS vigente;
- identidad y direccion de cliente;
- autenticador vigente y dentro del clock skew;
- autenticador no reutilizado.

## 4. TGS -> Client

Mensaje: `TGS_RESPONSE`

Payload externo: `CryptoEnvelope`

Plaintext cifrado para el cliente: `SecureTgsResponse`

`SecureTgsResponse` contiene:

- clave de sesion cliente-servicio;
- `ticketServiceEnvelope`.

`ticketServiceEnvelope` cifra un `TicketService` con el secreto del servicio.

## 5. Client -> Service

Mensaje: `SERVICE_REQUEST`

Payload: `SecureServiceRequest`

Contenido principal:

- `ticketServiceEnvelope`
- `clientAuthenticatorEnvelope`

El Service valida:

- `requestId` no repetido;
- servicio registrado;
- ticket de servicio vigente;
- identidad y direccion de cliente;
- autenticador vigente y dentro del clock skew;
- autenticador no reutilizado.

## 6. Service -> Client

Mensaje: `SERVICE_RESPONSE`

Payload externo: `CryptoEnvelope`

Plaintext cifrado para el cliente: `ServiceResponse`

El cliente descifra la respuesta con la clave de sesion cliente-servicio y
verifica que el tipo de respuesta sea el esperado.

## Errores Controlados

Los errores vuelven como `ERROR_RESPONSE` con payload `ErrorResponse`.

Casos cubiertos por pruebas unitarias, de componente o de integracion:

- flujo completo exitoso;
- replay y `requestId` repetido;
- servicio inexistente;
- cliente inexistente;
- ticket TGS expirado;
- ticket de servicio expirado;
- autenticador expirado o fuera de clock skew;
- ciphertext y `CryptoEnvelope` alterados;
- clave incorrecta;
- JSON vacio, truncado, malformado o con payload faltante;
- `MessageType` incorrecto;
- servidor no disponible;
- multiples clientes concurrentes.

## Ruta Legacy

La ruta legacy mantiene los payloads historicos:

- `HashMap<String,Object>`
- `SealedObject`
- `AESUtils`
- `ObjectInputStream` / `ObjectOutputStream`

Sus mappers (`Legacy*Mapper`) siguen disponibles para compatibilidad y pruebas,
pero no son el contrato principal del runtime modular.

## Pendiente

- Ejecutar la suite Maven completa en entorno con Maven.
- Eliminar legacy solo despues de pasar los gates obligatorios.
- Evaluar reemplazo del codec JSON manual por una biblioteca si se autoriza una
  dependencia.
