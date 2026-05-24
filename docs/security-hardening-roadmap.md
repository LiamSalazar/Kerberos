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
- incluye prueba y auditoria de concurrencia con evidencia versionada.
- soporta `AUTH_STORAGE_MODE=memory|sqlite|postgres`; SQLite es local y
  PostgreSQL queda preparado para RDS/cloud.
- incluye migraciones SQLite versionadas y `schema_version`.
- registra auditoria persistente local de flujos WebSocket en SQLite cuando se
  usa `AUTH_STORAGE_MODE=sqlite`.
- agrega CLI local para registrar/listar/habilitar/deshabilitar clientes y
  servicios.
- agrega `auth-websocket-gateway` como capa separada para futuras integraciones
  web, sin reemplazar la ruta TCP modular.
- agrega `auth-web-demo` como frontend local desacoplado que consume solamente
  el Gateway WebSocket.
- agrega `sample-login-app` como mini app vanilla para demostrar integracion
  tipo login.
- agrega sesiones opacas verificables en el Gateway para que una app externa no
  conceda acceso solamente por `FLOW_RESULT.success=true`.
- agrega `VERIFY_SESSION` y `LOGOUT_SESSION`, con sesiones en memoria, SQLite
  o PostgreSQL segun configuracion.
- agrega `SecretsProvider` para variables de entorno y AWS Secrets Manager.
- agrega health HTTP, logs JSON sanitizados y metricas basicas del Gateway.

El codigo legacy fisico ya fue retirado del proyecto principal. Fase 9 retiro
tambien `auth-transport/javaio` y `auth-transport/legacy`. El contexto
historico queda documentado en `docs/legacy-summary.md`.

## Riesgos Modulares Abiertos

- Los secretos por defecto siguen existiendo en modo `demo`.
- `AUTH_MODE=strict` valida secretos explicitos y puede resolver Secrets
  Manager, pero no agrega rotacion real.
- `InMemoryReplayCache` no es compartida entre procesos.
- SQLite local no reemplaza una estrategia productiva de persistencia, cifrado
  de secretos ni rotacion.
- PostgreSQL/RDS esta preparado, pero falta validarlo contra infraestructura real.
- La auditoria persistente registra eventos de alto nivel, no trazas completas
  ni observabilidad distribuida.
- `JsonMessageCodec` es un codec acotado al proyecto, no un parser JSON
  general-purpose auditado.
- No hay TLS ni autenticacion mutua de transporte.
- Los nombres de secretos actuales son `AUTH_DEMO_*`; `AUTH_MODE=strict`
  exige valores explicitos.
- El gateway WebSocket tiene pruebas unitarias, de componente y E2E real con
  cliente WebSocket y servidores modulares levantados dentro de Maven.
- La demo web no muestra secretos, tickets completos, ciphertexts ni material
  criptografico sensible.
- La comunicacion `ws://` sigue siendo solo local. Para cloud se requiere
  `wss://`, terminacion TLS y hardening operativo.
- No se agrego RSA, JWT ni autoridad certificadora; el modelo actual usa sesion
  opaca validada server-side por el Gateway.

## Prioridad Siguiente

1. Validar Docker en Linux y PostgreSQL real controlado.
2. Endurecer el canal WebSocket/frontend con TLS o autenticacion local si se
   autoriza.
3. Evaluar Jackson/Gson u otro JSON parser mantenido si el codec propio crece
   fuera de su alcance acotado.
4. Agregar TLS o una capa de transporte autenticada para la ruta modular.
5. Cargar secretos reales en Secrets Manager y definir rotacion.
6. Agregar pruebas E2E de navegador para la demo web si se autoriza tooling.
7. Evaluar TLS/WSS y gestion de secretos antes de cualquier despliegue cloud.

## Dependencia WebSocket

Se agrego `org.java-websocket:Java-WebSocket` porque Java estandar no provee un
servidor WebSocket simple para este caso. La dependencia se mantiene limitada al
modulo `auth-websocket-gateway` y evita introducir Spring Boot o un framework de
aplicacion completo.

No se agrego Jackson/Gson en esta fase. El codec JSON propio se mantiene porque
sigue acotado a DTOs del protocolo y mensajes planos del gateway, con pruebas de
JSON malformado, campos faltantes, tipos incorrectos y payload invalido.

## Dependencias Cloud

Fase 20 agrega `org.postgresql:postgresql` para `auth-storage-postgres` y AWS
SDK v2 `software.amazon.awssdk:secretsmanager` en `auth-core`. No se agrego ORM,
Spring Boot, RSA, JWT ni autoridad certificadora.

## Dependencia SQLite

Se agrego `org.xerial:sqlite-jdbc` en `auth-storage-sqlite` para probar
persistencia local sin servidor externo y sin ORM. La dependencia no
se usa para guardar secretos de produccion; solo habilita una integracion local
verificable con schema y seed demo.
