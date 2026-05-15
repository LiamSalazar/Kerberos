# Security Hardening Roadmap

Este documento describe riesgos reales y una ruta responsable de mejora. El
proyecto no debe presentarse como MIT Kerberos oficial ni como sistema listo
para produccion critica.

## Problemas Actuales Identificados

1. `AESUtils` legacy usa `AES/CBC/PKCS5Padding`.
2. `generateIv()` mantiene un IV fijo para compatibilidad con el formato actual.
3. El runtime legacy todavia usa `SealedObject`.
4. `Seguridad.Comunicacion` usa `ObjectInputStream/ObjectOutputStream`.
5. Aun existen defaults de secretos para demo local.
6. La replay cache es local en memoria y no distribuida.
7. Los modulos runtime nuevos aun no reemplazan la demo legacy.

## Riesgos De Seguridad

- CBC con IV fijo no ofrece la seguridad esperada para mensajes repetidos.
- Java serialization expone una superficie de deserializacion innecesaria.
- Secretos por defecto son aceptables solo para demo local.
- Replay cache local no protege despliegues con multiples instancias.
- La validacion de mensajes sigue muy acoplada a clases Java serializadas.

## Cambios Minimos De Esta Fase

- `AuthConfig` centraliza ids, puertos, duraciones, skew, replay window y
  secretos legacy de demo.
- `ReplayCache` e `InMemoryReplayCache` fueron agregados en `auth-core`.
- TGS y Service integran replay cache minima para autenticadores legacy.
- Logs sensibles fueron reducidos para no imprimir claves ni tickets
  descifrados completos.
- `auth-crypto` incluye `CryptoEnvelope`, `AeadCryptoService` y
  `AesGcmCryptoService`.
- Se agregaron pruebas unitarias para replay cache, mappers y AES-GCM.

## AES-GCM

La base moderna ya existe en `auth-crypto`. Usa:

- `AES/GCM/NoPadding`
- IV/nonce por mensaje
- tag de autenticacion de 128 bits
- `CryptoEnvelope` para transportar IV y ciphertext

No se migro de golpe el legacy porque el formato actual de `AESUtils` no
transporta IV junto al objeto cifrado. Cambiar `generateIv()` a aleatorio sin
un envelope romperia el descifrado actual.

## Replay Cache

Estado actual:

- `InMemoryReplayCache` registra claves con expiracion.
- TGS usa una clave compuesta con scope, TGS, cliente, IP y timestamp del
  autenticador.
- Service usa una clave compuesta con scope, servicio, cliente, IP y timestamp.
- La expiracion se limita por ticket o ventana de replay.

Pendiente:

- Usar `requestId`, `authenticatorId` o `ticketId` reales en todo el runtime.
- Agregar pruebas de integracion con replays reales sobre sockets.
- Evaluar backend compartido si existieran multiples instancias.

## Configuracion Externa

`AuthConfig` soporta defaults y variables de entorno. Los defaults preservan la
demo local, pero deben entenderse como compatibilidad, no como secretos reales.

Variables relevantes:

- `AUTH_AS_PORT`
- `AUTH_TGS_PORT`
- `AUTH_SERVICE_PORT`
- `AUTH_TICKET_TTL_MINUTES`
- `AUTH_ALLOWED_SKEW_SECONDS`
- `AUTH_REPLAY_WINDOW_SECONDS`
- `AUTH_LEGACY_CLIENT_SECRET`
- `AUTH_LEGACY_CLIENT_TGS_KEY`
- `AUTH_LEGACY_TGS_SECRET`
- `AUTH_LEGACY_CLIENT_SERVICE_KEY`
- `AUTH_LEGACY_SERVICE_SECRET`

Trabajo futuro:

- archivo local no versionado para desarrollo;
- validacion estricta de configuracion;
- separacion por ambientes;
- rotacion de secretos.

## ObjectInputStream/ObjectOutputStream

La eliminacion gradual de Java serialization es una prioridad tecnica. Ruta
recomendada:

1. Mantener mappers legacy mientras la demo siga viva.
2. Hacer que el runtime cree DTOs tipados antes de serializar.
3. Cambiar transporte a JSON, CBOR u otro formato validable.
4. Aplicar validacion de esquema.
5. Retirar `SealedObject` y objetos Java arbitrarios del protocolo.

## Prioridad De Correccion

1. Pruebas de integracion para replay en TGS y Service.
2. Migracion de respuestas AS/TGS/Service a `CryptoEnvelope`.
3. Reemplazo de `HashMap<String,Object>` por DTOs en runtime.
4. Sustitucion de Java serialization.
5. Configuracion externa completa.
6. Docker Compose para despliegue local reproducible.

## Docker

Docker no forma parte del hardening inmediato de esta fase. Se implementara mas
adelante, cuando existan runtimes modulares y una suite de pruebas de
integracion mas estable. En la version actual, la ejecucion local sin Docker es
un requisito explicito.

## Estado Honesto

Despues de esta fase, el proyecto queda mas serio y verificable:

- tiene contratos tipados;
- tiene mappers de compatibilidad;
- tiene replay cache inicial;
- tiene base AES-GCM probada;
- tiene configuracion centralizada;
- tiene documentacion ejecutable.

Pero aun no es production-ready. La seguridad completa requiere migrar el
runtime real a DTOs, AEAD, transporte validable y configuracion externa robusta.
