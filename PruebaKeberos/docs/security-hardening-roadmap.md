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

El codigo legacy fisico ya fue retirado del proyecto principal. Fase 9 retiro
tambien `auth-transport/javaio` y `auth-transport/legacy`. El contexto
historico queda documentado en `docs/legacy-summary.md`.

## Riesgos Modulares Abiertos

- Los secretos por defecto siguen existiendo en modo `demo/local`.
- `AUTH_MODE=strict` valida presencia de secretos, pero no agrega vault ni
  rotacion real.
- `InMemoryReplayCache` no es compartida entre procesos.
- `JsonMessageCodec` es un codec acotado al proyecto, no un parser JSON
  general-purpose auditado.
- No hay TLS ni autenticacion mutua de transporte.
- Los nombres de secretos actuales son `AUTH_DEMO_*`; `AUTH_MODE=strict`
  exige valores explicitos.
- El gateway WebSocket tiene pruebas unitarias, de componente y E2E real con
  cliente WebSocket y servidores modulares levantados dentro de Maven.

## Prioridad Siguiente

1. Conectar un frontend futuro al contrato WebSocket documentado.
2. Evaluar Jackson/Gson u otro JSON parser mantenido si el codec propio crece
   fuera de su alcance acotado.
3. Agregar TLS o una capa de transporte autenticada para la ruta modular.
4. Docker y Docker Compose solo en una fase futura de despliegue.
5. Frontend solo en una fase futura especifica.

## Dependencia WebSocket

Se agrego `org.java-websocket:Java-WebSocket` porque Java estandar no provee un
servidor WebSocket simple para este caso. La dependencia se mantiene limitada al
modulo `auth-websocket-gateway` y evita introducir Spring Boot o un framework de
aplicacion completo.

No se agrego Jackson/Gson en esta fase. El codec JSON propio se mantiene porque
sigue acotado a DTOs del protocolo y mensajes planos del gateway, con pruebas de
JSON malformado, campos faltantes, tipos incorrectos y payload invalido.
