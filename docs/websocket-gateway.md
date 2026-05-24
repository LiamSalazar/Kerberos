# WebSocket Gateway

`auth-websocket-gateway` expone una API WebSocket separada para apps externas y
demos locales. No reemplaza AS, TGS ni Service; solo traduce mensajes WebSocket
a la ruta modular AS -> TGS -> Service por TCP/JSON.

No usa Spring Boot, no usa JWT, no usa RSA y no expone tickets, claves,
ciphertexts ni secretos al frontend.

## Contract

Entradas:

- `START_AUTH_FLOW`
- `VERIFY_SESSION`
- `LOGOUT_SESSION`
- `PING`

Salidas:

- `FLOW_EVENT`
- `FLOW_RESULT`
- `SESSION_VALID`
- `SESSION_INVALID`
- `SESSION_LOGGED_OUT`
- `ERROR`
- `PONG`

`START_AUTH_FLOW` requiere:

```json
{"type":"START_AUTH_FLOW","requestId":"app-1","clientId":"1","serviceId":"1"}
```

Si el flujo es exitoso, `FLOW_RESULT` incluye una sesion opaca:

```json
{
  "type": "FLOW_RESULT",
  "requestId": "app-1",
  "success": true,
  "sessionId": "opaque-session-id",
  "sessionExpiresAt": "2026-05-23T17:00:00Z",
  "serviceMessage": "MODULAR AUTH EXITOSO"
}
```

La app no debe conceder acceso solo por `success=true`. Debe verificar:

```json
{"type":"VERIFY_SESSION","requestId":"verify-1","sessionId":"opaque-session-id","clientId":"1","serviceId":"1"}
```

Respuesta valida:

```json
{"type":"SESSION_VALID","requestId":"verify-1","valid":true,"clientId":"1","serviceId":"1","expiresAt":"2026-05-23T17:00:00Z"}
```

Respuesta invalida:

```json
{"type":"SESSION_INVALID","requestId":"verify-1","valid":false,"reason":"EXPIRED"}
```

Logout:

```json
{"type":"LOGOUT_SESSION","requestId":"logout-1","sessionId":"opaque-session-id"}
```

## Errors

Errores tipados:

- `INVALID_JSON`
- `UNKNOWN_MESSAGE_TYPE`
- `MISSING_REQUIRED_FIELD`
- `CLIENT_NOT_FOUND`
- `SERVICE_NOT_FOUND`
- `FLOW_FAILED`
- `RATE_LIMITED`
- `ORIGIN_NOT_ALLOWED`
- `SESSION_NOT_FOUND`
- `SESSION_EXPIRED`
- `SESSION_REVOKED`
- `SESSION_CLIENT_MISMATCH`
- `SESSION_SERVICE_MISMATCH`
- `SESSION_REQUIRED`
- `INVALID_SESSION_REQUEST`

`SESSION_INVALID.reason` usa:

- `NOT_FOUND`
- `EXPIRED`
- `REVOKED`
- `CLIENT_MISMATCH`
- `SERVICE_MISMATCH`

## Security Controls

- `AUTH_ALLOWED_ORIGINS` limita origenes WebSocket cuando se configura.
- Hay rate limit simple por conexion.
- Hay timeout de flujo.
- La sesion opaca vive en memoria o SQLite del sistema de autenticacion.
- `ws://` es solo para desarrollo local; en cloud se debe usar `wss://`.

## Configuration

- `AUTH_WS_HOST`: default `127.0.0.1`.
- `AUTH_WS_PORT`: default `2800`.
- `AUTH_ALLOWED_ORIGINS`: lista separada por comas.
- `AUTH_SESSION_TTL_SECONDS`: TTL operativo de sesion.
- `AUTH_SESSION_MAX_TTL_SECONDS`: maximo defensivo de sesion.
- `AUTH_SESSION_STORAGE_MODE`: `memory`, `sqlite` o `postgres`.
- `AUTH_REQUIRE_SESSION_VERIFY`: default efectivo `true` en strict.

## Tests

```bash
mvn -pl auth-websocket-gateway -am test
```

La suite cubre JSON invalido, tipos desconocidos, campos faltantes, origen
permitido/rechazado, rate limit, timeout, flujo exitoso, sesion creada, sesion
verificada, logout y errores controlados.
