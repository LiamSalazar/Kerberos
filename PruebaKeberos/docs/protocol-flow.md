# Protocol Flow

Este documento describe el flujo actual y el estado de migracion de cada
mensaje. El runtime principal sigue usando payloads legacy, pero los DTOs y
mappers ya preparan la ruta modular.

## 1. Client -> AS

Objetivo: pedir un Ticket Granting Ticket.

Payload legacy:

- `[Id-c]`
- `[Id-tgs]`
- `[TimeStamp-1]`

DTO:

- `AsRequest`

Mapper:

- `LegacyAsRequestMapper`

Estado: integrado en el flujo legacy. El cliente crea `AsRequest`, lo convierte
a `HashMap` legacy y el AS lo reconstruye como DTO.

## 2. AS -> Client

Objetivo: entregar clave de sesion cliente-TGS y Ticket TGS.

Payload legacy:

- `[K-c_tgs]`
- `[Id-tgs]`
- `[TimeStamp-2]`
- `[TiempoVida-2]`
- `[Ticket-tgs]`

DTO:

- `AsResponse`
- `TicketTgs`

Mapper:

- `LegacyAsResponseMapper`

Estado: mapper y pruebas implementados. El AS construye un `AsResponse` y un
`TicketTgs` DTO, pero envia el ticket real como `SealedObject` legacy para no
romper compatibilidad.

## 3. Client -> TGS

Objetivo: pedir ticket para un servicio concreto.

Payload legacy:

- `[Id-v]`
- `[Ticket-tgs]`
- `[Autentificador-c]`

DTO:

- `TgsRequest`
- `TicketTgs`
- `ClientAuthenticator`

Mapper:

- `LegacyTgsRequestMapper`

Estado: integrado parcialmente. El cliente construye `TgsRequest` y baja el
mensaje a mapa legacy; el ticket y el autenticador siguen viajando como
`SealedObject`.

## 4. TGS -> Client

Objetivo: entregar clave de sesion cliente-servicio y ticket de servicio.

Payload legacy:

- `[K-c_v]`
- `[Id-v]`
- `[TimeStamp-4]`
- `[TiempoVida-4]`
- `[Ticket-v]`

DTO:

- `TgsResponse`
- `TicketService`

Mapper:

- `LegacyTgsResponseMapper`

Estado: integrado parcialmente. El TGS construye `TgsResponse` y
`TicketService`, despues baja a mapa legacy y conserva `[Ticket-v]` como
`SealedObject`.

## 5. Client -> Service

Objetivo: presentar ticket de servicio y autenticador.

Payload legacy:

- `[Ticket-v]`
- `[Autentificador-c]`

DTO:

- `ServiceRequest`
- `TicketService`
- `ClientAuthenticator`

Mapper:

- `LegacyServiceRequestMapper`

Estado: integrado parcialmente. El cliente construye `ServiceRequest` y baja el
mensaje a mapa legacy; Service todavia valida ticket y autenticador legacy
descifrados, con replay cache para rechazar reuso basico.

## 6. Service -> Client

Objetivo: confirmar acceso al servicio.

Payload legacy:

- `[TimeStamp-incrementada]`
- `[Servicio]`

DTO:

- `ServiceResponse`

Mapper:

- `LegacyServiceResponseMapper`

Estado: integrado parcialmente. Service construye `ServiceResponse`, lo baja a
mapa legacy y mantiene la respuesta cifrada con `AESUtils` por compatibilidad.

## Resumen De Migracion

| Mensaje | DTO | Mapper | Integrado al runtime |
| --- | --- | --- | --- |
| Client -> AS | Si | Si | Si |
| AS -> Client | Si | Si | Parcial, DTO antes de mapa legacy |
| Client -> TGS | Si | Si | Parcial, DTO antes de mapa legacy |
| TGS -> Client | Si | Si | Parcial, DTO antes de mapa legacy |
| Client -> Service | Si | Si | Parcial, DTO antes de mapa legacy |
| Service -> Client | Si | Si | Parcial, DTO antes de mapa legacy |

## Pendiente

- Llevar la validacion interna de TGS y Service a DTOs, no solo a mapas bridge.
- Migrar tickets cifrados a `CryptoEnvelope`.
- Agregar `ticketId` real al runtime legacy o moverlo al runtime modular.
- Reemplazar Java serialization por un formato validable.
