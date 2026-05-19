# Security Hardening Roadmap

Este proyecto es una pieza de portafolio inspirada en Kerberos 4. No es MIT
Kerberos oficial y no esta listo para produccion critica.

## Estado Actual

La ruta modular es la ruta principal y ya reduce riesgos importantes frente al
legacy:

- usa DTOs y `ProtocolEnvelope`;
- transporta JSON sobre TCP;
- cifra tickets, autenticadores y respuestas con AES-GCM;
- transporta IV/nonce y ciphertext en `CryptoEnvelope`;
- usa associated data estable por tipo de objeto;
- bloquea `requestId` y autenticadores repetidos con `InMemoryReplayCache`;
- valida expiracion y clock skew en TGS y Service;
- tiene timeouts y limite de tamano de mensaje en transporte TCP;
- tiene modo `AUTH_MODE=strict` para rechazar secretos demo;
- incluye auditoria reproducible de ejecucion/desempeno.

La ruta legacy sigue disponible porque la eliminacion requiere Maven validado.

## Riesgos Legacy

- `AESUtils` usa `AES/CBC/PKCS5Padding`.
- `AESUtils.generateIv()` mantiene IV fijo por compatibilidad.
- `SealedObject` y Java serialization siguen en la demo legacy.
- `HashMap<String,Object>` sigue presente en el contrato legacy.

Estos puntos no se cambiaron para no romper la referencia historica antes del
gate de eliminacion.

## Riesgos Modulares Abiertos

- Los secretos por defecto siguen existiendo en modo `demo/local`.
- `AUTH_MODE=strict` valida presencia de secretos, pero no agrega vault ni
  rotacion real.
- `InMemoryReplayCache` no es compartida entre procesos.
- `JsonMessageCodec` es un codec acotado al proyecto, no un parser JSON
  general-purpose auditado.
- No hay TLS ni autenticacion mutua de transporte.
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

Las pruebas cubren descifrado correcto, associated data incorrecta, ciphertext
corrupto, envelope alterado y clave incorrecta.

## Auditorias

- `docs/audits/legacy-dependency-audit.md`: confirma que `auth-*` no importa
  `Kerberos.*` ni `Seguridad.*`.
- `docs/audits/legacy-removal-blockers.md`: explica por que legacy no se borro
  en esta fase.
- `docs/audits/latest-run.md` y `docs/audits/latest-run.json`: evidencia de
  ejecucion modular con latencias y throughput aproximado.

## Prioridad Siguiente

1. Instalar Maven o asegurar PATH correcto y ejecutar `mvn test`.
2. Si Maven pasa, eliminar `Kerberos/` y `Seguridad/` del runtime principal.
3. Retirar adaptadores `auth-transport/legacy` y `auth-transport/javaio` cuando
   ya no haya objetivo de compatibilidad.
4. Evaluar Jackson/Gson u otro JSON parser mantenido si se autoriza una sola
   dependencia.
5. Agregar TLS o una capa de transporte autenticada para la ruta modular.
6. Docker y Docker Compose solo en una fase futura de despliegue.
7. WebSockets y frontend solo en fases futuras especificas.
