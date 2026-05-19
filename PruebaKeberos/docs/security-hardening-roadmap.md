# Security Hardening Roadmap

Este proyecto es una pieza de portafolio inspirada en Kerberos 4. No es MIT
Kerberos oficial y no esta listo para produccion critica.

## Estado Actual

La ruta modular es la ruta principal:

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

Las carpetas legacy historicas fueron retiradas en Fase 8.1. El contexto queda
documentado en `docs/legacy-summary.md`.

## Riesgos Modulares Abiertos

- Los secretos por defecto siguen existiendo en modo `demo/local`.
- `AUTH_MODE=strict` valida presencia de secretos, pero no agrega vault ni
  rotacion real.
- `InMemoryReplayCache` no es compartida entre procesos.
- `JsonMessageCodec` es un codec acotado al proyecto, no un parser JSON
  general-purpose auditado.
- No hay TLS ni autenticacion mutua de transporte.
- Persisten adaptadores historicos internos en `auth-transport` para pruebas y
  compatibilidad documental.

## Prioridad Siguiente

1. Retirar o renombrar adaptadores historicos internos de `auth-transport`.
2. Renombrar variables `AUTH_LEGACY_*` a nombres modulares.
3. Evaluar Jackson/Gson u otro JSON parser mantenido si se autoriza una sola
   dependencia.
4. Agregar TLS o una capa de transporte autenticada para la ruta modular.
5. Docker y Docker Compose solo en una fase futura de despliegue.
6. WebSockets y frontend solo en fases futuras especificas.
