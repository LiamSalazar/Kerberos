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
- agrega `auth-websocket-gateway` como capa separada para futuras integraciones
  web, sin reemplazar la ruta TCP modular.

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
- `AUTH_LEGACY_*` existe solo como alias temporal de compatibilidad; los nombres
  principales son `AUTH_DEMO_*`.
- El gateway WebSocket tiene pruebas unitarias de mensajes y flujo controlado,
  pero aun no tiene prueba end-to-end con cliente WebSocket real y servidores
  modulares levantados en la misma suite.

## Prioridad Siguiente

1. Retirar aliases `AUTH_LEGACY_*` cuando ya no se necesite compatibilidad.
2. Agregar pruebas WebSocket end-to-end con servicios reales si se decide
   ampliar la suite.
3. Evaluar Jackson/Gson u otro JSON parser mantenido si el codec propio crece
   fuera de su alcance acotado.
4. Agregar TLS o una capa de transporte autenticada para la ruta modular.
5. Docker y Docker Compose solo en una fase futura de despliegue.
6. Frontend solo en una fase futura especifica.

## Dependencia WebSocket

Se agrego `org.java-websocket:Java-WebSocket` porque Java estandar no provee un
servidor WebSocket simple para este caso. La dependencia se mantiene limitada al
modulo `auth-websocket-gateway` y evita introducir Spring Boot o un framework de
aplicacion completo.

No se agrego Jackson/Gson en esta fase. El codec JSON propio se mantiene porque
sigue acotado a DTOs del protocolo y mensajes planos del gateway, con pruebas de
JSON malformado, campos faltantes, tipos incorrectos y payload invalido.
