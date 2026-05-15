# Architecture

El proyecto combina una demo legacy funcional con una migracion modular en
progreso. La prioridad de esta fase es estabilizar sin romper el flujo
AS -> TGS -> Service -> Client.

## Arquitectura Legacy

La demo original vive en:

- `Kerberos/`
- `Seguridad/`

Responsabilidades principales:

| Clase | Responsabilidad |
| --- | --- |
| `Kerberos.AuthenticationServer` | Recibe solicitud inicial y emite Ticket TGS |
| `Kerberos.TicketGrantingServer` | Valida Ticket TGS y emite ticket de servicio |
| `Kerberos.ServiceServer` | Valida ticket de servicio y autenticador |
| `Kerberos.Client` | Ejecuta el flujo completo como cliente |
| `Kerberos.AESUtils` | Cifrado legacy basado en `SealedObject` |
| `Seguridad.Comunicacion` | Envio/recepcion con `ObjectInputStream/ObjectOutputStream` |
| `Seguridad.Conexiones` | Conexion socket simple |

Esta capa sigue siendo la ruta ejecutable principal. No debe eliminarse sin una
migracion completa y probada.

## Arquitectura Modular Propuesta

La migracion separa responsabilidades:

| Modulo | Responsabilidad | Estado |
| --- | --- | --- |
| `auth-core` | DTOs, contratos, configuracion, replay cache | Implementado parcialmente |
| `auth-transport` | Transporte y mappers de compatibilidad legacy | Implementado parcialmente |
| `auth-crypto` | Servicios criptograficos modernos | Base AES-GCM implementada |
| `auth-as` | Runtime modular del AS | Pendiente |
| `auth-tgs` | Runtime modular del TGS | Pendiente |
| `auth-service` | Runtime modular del servicio protegido | Pendiente |
| `auth-client-sdk` | API cliente para consumir el protocolo | Pendiente |
| `docs` | Documentacion tecnica | Activa |

## Que Esta Implementado

En `auth-core`:

- DTOs principales del protocolo.
- `AuthConfig` con defaults de demo y variables de entorno.
- `ReplayCache` e `InMemoryReplayCache`.

En `auth-transport`:

- `JavaObjectTransport`.
- `LegacyAsRequestMapper`.
- `LegacyAsResponseMapper`.
- `LegacyTgsRequestMapper`.
- `LegacyTgsResponseMapper`.
- `LegacyServiceRequestMapper`.
- `LegacyServiceResponseMapper`.

En `auth-crypto`:

- `CryptoEnvelope`.
- `AeadCryptoService`.
- `AesGcmCryptoService`.

En legacy:

- El AS ya usa `AsRequest` a traves de `LegacyAsRequestMapper`.
- TGS y Service integran replay cache minima.
- Logs sensibles fueron reducidos.
- Configuracion basica se lee desde `AuthConfig` con defaults compatibles.

## Que Sigue En Migracion

- Los tickets reales aun viajan como `SealedObject`.
- Las respuestas AS/TGS/Service aun usan mapas legacy cifrados.
- Los mappers ya existen, pero no reemplazan todo el runtime.
- AES-GCM esta listo en `auth-crypto`, pero no cifra aun los mensajes legacy.
- La replay cache usa claves compuestas derivadas del autenticador legacy.

## Que Falta

- Migrar tickets a DTOs versionados con `ticketId`.
- Sustituir `ObjectInputStream/ObjectOutputStream`.
- Integrar `CryptoEnvelope` en AS, TGS y Service.
- Agregar pruebas de integracion end-to-end.
- Convertir los modulos runtime `auth-as`, `auth-tgs` y `auth-service` en
  aplicaciones reales.
- Externalizar configuracion con archivo local opcional y variables de entorno.

## Por Que No Docker Todavia

Docker seria util para despliegue reproducible, pero en esta fase el objetivo es
estabilizar funcionalidad local, migracion de protocolo, pruebas y hardening
minimo. Introducir Docker ahora agregaria superficie operacional antes de cerrar
la base tecnica.

Docker queda para una fase posterior, cuando existan runtimes modulares y
pruebas de integracion mas completas.
