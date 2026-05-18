# Security Hardening Roadmap

Este proyecto es una pieza de portafolio inspirada en Kerberos 4. No es MIT
Kerberos oficial y no esta listo para produccion critica.

## Estado Actual

La ruta modular ya reduce riesgos importantes frente al legacy:

- usa DTOs y `ProtocolEnvelope`;
- transporta JSON sobre TCP;
- cifra tickets, autenticadores y respuestas con AES-GCM;
- transporta IV/nonce y ciphertext en `CryptoEnvelope`;
- bloquea `requestId` y autenticadores repetidos con `InMemoryReplayCache`;
- incluye pruebas unitarias y una prueba de integracion modular.

La ruta legacy sigue disponible y conserva sus limitaciones historicas.

## Riesgos Legacy

- `AESUtils` usa `AES/CBC/PKCS5Padding`.
- `AESUtils.generateIv()` mantiene IV fijo por compatibilidad.
- `SealedObject` y Java serialization siguen en la demo legacy.
- `HashMap<String,Object>` sigue presente en el contrato legacy.

Estos puntos no se cambiaron para no romper la demo historica.

## Riesgos Modulares Abiertos

- Los secretos por defecto siguen siendo de demo local.
- `InMemoryReplayCache` no es compartida entre procesos.
- `JsonMessageCodec` es un codec acotado al proyecto, no un parser JSON
  general-purpose auditado.
- No hay gestion real de rotacion de claves.
- No hay autenticacion mutua de transporte ni TLS.
- Maven no pudo ejecutarse en esta sesion porque `mvn` no estaba en PATH.

## AES-GCM Modular

La ruta modular usa:

- `AesGcmCryptoService`
- `AesKeyDerivation`
- `SessionKeys`
- `CryptoEnvelope`
- `SecureAad`

Se cifran:

- `TicketTgs` con secreto del TGS;
- `TicketService` con secreto del servicio;
- `ClientAuthenticator` con clave de sesion;
- `SecureAsResponse` con secreto del cliente;
- `SecureTgsResponse` con clave cliente-TGS;
- `ServiceResponse` con clave cliente-servicio.

## Replay

TGS y Service registran:

- `requestId` por ventana de replay;
- autenticadores por ticket y `authenticatorId`.

Pendiente:

- backend compartido si se ejecutan multiples instancias;
- metricas de replay;
- pruebas adicionales para expiracion de tickets.

## Prioridad Siguiente

1. Ejecutar y mantener `mvn test` en entorno con Maven disponible.
2. Separar secretos demo hacia configuracion local no versionada.
3. Evaluar Jackson/Gson u otro JSON parser mantenido si se autoriza una sola
   dependencia.
4. Agregar TLS o una capa de transporte autenticada para la ruta modular.
5. Ampliar pruebas de expiracion, corrupcion de ciphertext y errores de red.
6. Docker y Docker Compose solo en una fase futura de despliegue.
7. WebSockets y frontend solo en fases futuras especificas.
