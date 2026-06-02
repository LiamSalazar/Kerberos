# Frontend Contract

This contract describes how `auth-web-demo`, `sample-login-app`, and future UIs
must communicate with `auth-websocket-gateway`.

The Gateway does not replace AS, TGS, or Service. It does not expose secrets,
keys, tickets, ciphertexts, or internal payloads to the client.

## Local URL

```text
ws://127.0.0.1:2800
```

`ws://` is only for local development. In cloud, publish `wss://`.

## Input Messages

### START_AUTH_FLOW

```json
{"type":"START_AUTH_FLOW","requestId":"front-req-1","clientId":"1","serviceId":"1"}
```

### VERIFY_SESSION

```json
{"type":"VERIFY_SESSION","requestId":"verify-1","sessionId":"opaque-session-id","clientId":"1","serviceId":"1"}
```

### LOGOUT_SESSION

```json
{"type":"LOGOUT_SESSION","requestId":"logout-1","sessionId":"opaque-session-id"}
```

### PING

```json
{"type":"PING","requestId":"ping-1"}
```

## Output Messages

### FLOW_EVENT

```json
{"type":"FLOW_EVENT","requestId":"front-req-1","stage":"AS_RESPONSE_RECEIVED","message":"TGT emitido"}
```

### FLOW_RESULT

Successful result:

```json
{
  "type": "FLOW_RESULT",
  "requestId": "front-req-1",
  "success": true,
  "serviceMessage": "MODULAR AUTH EXITOSO",
  "sessionId": "opaque-session-id",
  "sessionExpiresAt": "2026-05-23T17:00:00Z",
  "asMillis": 4,
  "tgsMillis": 3,
  "serviceMillis": 3,
  "totalMillis": 12
}
```

Failed result:

```json
{"type":"FLOW_RESULT","requestId":"front-req-2","success":false,"errorType":"SERVICE_NOT_FOUND","serviceMessage":"TGS_UNKNOWN_SERVICE"}
```

`FLOW_RESULT.success=true` is not final authorization. It only means the Gateway
created a server-side opaque session after a successful flow.

### SESSION_VALID

```json
{"type":"SESSION_VALID","requestId":"verify-1","valid":true,"clientId":"1","serviceId":"1","expiresAt":"2026-05-23T17:00:00Z"}
```

### SESSION_INVALID

```json
{"type":"SESSION_INVALID","requestId":"verify-1","valid":false,"reason":"EXPIRED"}
```

### SESSION_LOGGED_OUT

```json
{"type":"SESSION_LOGGED_OUT","requestId":"logout-1","message":"Sesion revocada"}
```

### ERROR

```json
{"type":"ERROR","errorType":"MISSING_REQUIRED_FIELD","message":"Campo requerido faltante: serviceId","success":false}
```

## Recommended Frontend Flow

1. Open a WebSocket to the Gateway.
2. Send `START_AUTH_FLOW`.
3. Render `FLOW_EVENT` as progress.
4. If `FLOW_RESULT.success === true`, store `sessionId` only in memory.
5. Send `VERIFY_SESSION`.
6. Open the protected area only if `SESSION_VALID` arrives.
7. On logout, send `LOGOUT_SESSION` and clear local state.

## Error Types

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

`SESSION_INVALID.reason` uses `NOT_FOUND`, `EXPIRED`, `REVOKED`,
`CLIENT_MISMATCH`, or `SERVICE_MISMATCH`.

## Security

The frontend must not receive or request demo secrets, session keys, full
tickets, `CryptoEnvelope`, ciphertexts, or internal payloads. The opaque session
can be shown partially masked for local debugging, but it must not be treated as
a self-verifiable token: the authority is the Gateway.
