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

Estado: mapper y pruebas implementados. El runtime legacy aun construye y cifra
el mapa directamente por compatibilidad.

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

Estado: mapper y pruebas implementados. El runtime legacy aun envia
`SealedObject` para ticket y autenticador.

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

Estado: mapper y pruebas implementados. El runtime legacy aun responde con mapa
cifrado por compatibilidad.

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

Estado: mapper y pruebas implementados. Service valida el payload legacy y ahora
usa replay cache para rechazar reuso basico del autenticador.

## 6. Service -> Client

Objetivo: confirmar acceso al servicio.

Payload legacy:

- `[TimeStamp-incrementada]`
- `[Servicio]`

DTO:

- `ServiceResponse`

Mapper:

- `LegacyServiceResponseMapper`

Estado: mapper y pruebas implementados. El runtime legacy aun responde con mapa
cifrado.

## Resumen De Migracion

| Mensaje | DTO | Mapper | Integrado al runtime |
| --- | --- | --- | --- |
| Client -> AS | Si | Si | Si |
| AS -> Client | Si | Si | Parcial |
| Client -> TGS | Si | Si | No, solo preparado |
| TGS -> Client | Si | Si | No, solo preparado |
| Client -> Service | Si | Si | No, replay cache integrada en validacion |
| Service -> Client | Si | Si | No, solo preparado |

## Pendiente

- Hacer que AS, TGS y Service construyan DTOs primero y mapas legacy despues.
- Migrar tickets cifrados a `CryptoEnvelope`.
- Agregar `ticketId` real al runtime legacy o moverlo al runtime modular.
- Reemplazar Java serialization por un formato validable.
