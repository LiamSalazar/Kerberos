# WebSocket Gateway

`auth-websocket-gateway` exposes a separate WebSocket API for external apps and
local demos. It does not replace AS, TGS, or Service; it only translates
WebSocket messages into the modular AS -> TGS -> Service path over TCP/JSON.

It does not use Spring Boot, JWT, or RSA, and it does not expose tickets, keys,
ciphertexts, or secrets to the frontend.

## Contract

Inputs:

- `START_AUTH_FLOW`
- `VERIFY_SESSION`
- `LOGOUT_SESSION`
- `PING`

Outputs:

- `FLOW_EVENT`
- `FLOW_RESULT`
- `SESSION_VALID`
- `SESSION_INVALID`
- `SESSION_LOGGED_OUT`
- `ERROR`
- `PONG`

`START_AUTH_FLOW` requires:

```json
{"type":"START_AUTH_FLOW","requestId":"app-1","clientId":"1","serviceId":"1"}
```

If the flow succeeds, `FLOW_RESULT` includes an opaque session:

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

The app must not grant access only because `success=true`. It must verify:

```json
{"type":"VERIFY_SESSION","requestId":"verify-1","sessionId":"opaque-session-id","clientId":"1","serviceId":"1"}
```

Valid response:

```json
{"type":"SESSION_VALID","requestId":"verify-1","valid":true,"clientId":"1","serviceId":"1","expiresAt":"2026-05-23T17:00:00Z"}
```

Invalid response:

```json
{"type":"SESSION_INVALID","requestId":"verify-1","valid":false,"reason":"EXPIRED"}
```

Logout:

```json
{"type":"LOGOUT_SESSION","requestId":"logout-1","sessionId":"opaque-session-id"}
```

## Errors

Typed errors:

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

`SESSION_INVALID.reason` uses:

- `NOT_FOUND`
- `EXPIRED`
- `REVOKED`
- `CLIENT_MISMATCH`
- `SERVICE_MISMATCH`

## Security Controls

- `AUTH_ALLOWED_ORIGINS` limits WebSocket origins when configured.
- There is a simple per-connection rate limit.
- There is a flow timeout.
- The opaque session lives in memory, SQLite, or PostgreSQL inside the authentication system.
- `ws://` is only for local development; use `wss://` in cloud.

## Configuration

- `AUTH_WS_HOST`: default `127.0.0.1`.
- `AUTH_WS_PORT`: default `2800`.
- `AUTH_ALLOWED_ORIGINS`: comma-separated list.
- `AUTH_SESSION_TTL_SECONDS`: operational session TTL.
- `AUTH_SESSION_MAX_TTL_SECONDS`: defensive maximum session TTL.
- `AUTH_SESSION_STORAGE_MODE`: `memory`, `sqlite`, or `postgres`.
- `AUTH_REQUIRE_SESSION_VERIFY`: effective default `true` in strict mode.

## Tests

```bash
mvn -pl auth-websocket-gateway -am test
```

The suite covers invalid JSON, unknown types, missing fields, allowed/rejected
origin, rate limit, timeout, successful flow, created session, verified session,
logout, and controlled errors.
