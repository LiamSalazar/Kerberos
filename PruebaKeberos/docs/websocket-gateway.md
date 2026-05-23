# WebSocket Gateway

`auth-websocket-gateway` expone una capa WebSocket separada para clientes web.
No reemplaza los servicios modulares principales: AS, TGS y Service siguen
ejecutandose como procesos TCP/JSON.

`auth-web-demo` consume este gateway como demo local desacoplada. El gateway no
usa Spring Boot y puede ejecutarse sin Docker o dentro de Docker Compose local.

## Dependencia

El modulo usa `org.java-websocket:Java-WebSocket`. Se eligio porque Java
estandar no incluye un servidor WebSocket simple para este caso y la alternativa
mantiene el alcance pequeno frente a frameworks completos.

No se agrego una dependencia JSON nueva. Los mensajes del gateway son planos y
el codec propio esta probado para JSON invalido, tipos desconocidos y campos
mal tipados.

## Flujo

1. Un cliente WebSocket conecta al gateway.
2. El cliente envia `START_AUTH_FLOW`.
3. El gateway usa `AuthClient` para ejecutar AS -> TGS -> Service por TCP/JSON.
4. El gateway emite `FLOW_EVENT` por etapa.
5. El gateway responde `FLOW_RESULT` con estado final y latencias.

El contrato WebSocket es transparente al modo de storage. Si AS, TGS y Service
se levantan con `AUTH_STORAGE_MODE=sqlite`, el Gateway mantiene el mismo
contrato y ademas registra auditoria persistente local en SQLite.

## Mensajes

Entrada:

- `START_AUTH_FLOW`
- `PING`

Salida:

- `FLOW_EVENT`
- `FLOW_RESULT`
- `ERROR`
- `PONG`

Ejemplo:

```json
{"type":"START_AUTH_FLOW","requestId":"manual-1","clientId":"1","serviceId":"1"}
```

Campos requeridos para `START_AUTH_FLOW`:

- `type`
- `requestId`
- `clientId`
- `serviceId`

Errores tipados:

- `INVALID_JSON`
- `UNKNOWN_MESSAGE_TYPE`
- `MISSING_REQUIRED_FIELD`
- `CLIENT_NOT_FOUND`
- `SERVICE_NOT_FOUND`
- `FLOW_FAILED`
- `RATE_LIMITED`
- `ORIGIN_NOT_ALLOWED`

Eventos esperados:

- `FLOW_STARTED`
- `AS_REQUEST_SENT`
- `AS_RESPONSE_RECEIVED`
- `TGS_REQUEST_SENT`
- `TGS_RESPONSE_RECEIVED`
- `SERVICE_REQUEST_SENT`
- `SERVICE_RESPONSE_RECEIVED`
- `FLOW_SUCCESS`
- `FLOW_ERROR`

## Ejecucion Local

Levanta primero AS, TGS y Service:

```cmd
scripts\run-as.bat
scripts\run-tgs.bat
scripts\run-service.bat
```

Luego:

```cmd
scripts\run-websocket-gateway.bat
```

Linux/macOS:

```bash
scripts/run-as.sh
scripts/run-tgs.sh
scripts/run-service.sh
scripts/run-websocket-gateway.sh
```

Variables:

- `AUTH_WS_HOST`: default `127.0.0.1`.
- `AUTH_WS_PORT`: default `2800`.
- `AUTH_ALLOWED_ORIGINS`: lista separada por comas. Si se configura, el
  `Origin` WebSocket debe coincidir exactamente.

## Prueba Manual

Con una herramienta externa como `websocat`:

```bash
websocat ws://127.0.0.1:2800
```

Enviar:

```json
{"type":"PING","requestId":"ping-1"}
```

o:

```json
{"type":"START_AUTH_FLOW","requestId":"manual-1","clientId":"1","serviceId":"1"}
```

## Pruebas Maven

```bash
mvn -pl auth-websocket-gateway -am test
```

La suite cubre serializacion de mensajes, tipos desconocidos, JSON invalido,
campos faltantes, origen permitido/rechazado, rate limit, timeout, flujo exitoso
con cliente real WebSocket y error controlado cuando los servicios no estan
disponibles.

## Seguridad Del Contrato

Los eventos del gateway son deliberadamente didacticos y de bajo detalle. No
envian tickets cifrados, claves de sesion, secretos demo, ciphertexts ni
payloads completos al cliente WebSocket. El frontend debe tratar `FLOW_EVENT`
como telemetria de estado y `FLOW_RESULT` como resultado final de alto nivel.

Ver tambien `docs/frontend-contract.md`.

## Demo Frontend Local

Con AS, TGS, Service y Gateway levantados:

```cmd
scripts\run-web-demo.bat
```

Luego abrir:

```text
http://127.0.0.1:5173
```

La UI envia `START_AUTH_FLOW`, procesa `FLOW_EVENT`, `FLOW_RESULT`, `ERROR` y
`PONG`, y muestra solo informacion de alto nivel.

## Sample Login App

`sample-login-app` usa el mismo contrato WebSocket para simular una app real con
login y zona protegida:

```cmd
scripts\run-sample-login-app.bat
```

Default:

```text
http://127.0.0.1:5174
```

Ver `docs/sample-login-app.md`.

## Auditoria SQLite

Con `AUTH_STORAGE_MODE=sqlite`, el Gateway aplica migraciones y registra eventos
seguros en `auth_audit_events`.

Consultar:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit list --limit 20
```

## Limites

- El gateway no levanta AS/TGS/Service automaticamente.
- El gateway no administra clientes ni servicios; solo registra auditoria
  persistente cuando se configura SQLite.
- No hay TLS ni autenticacion mutua en el canal WebSocket.
- No hay Docker ni despliegue web en esta fase.
