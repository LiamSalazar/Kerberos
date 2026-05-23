# Frontend Contract

Este contrato describe como `auth-web-demo`, `sample-login-app` y futuras UIs
deben comunicarse con `auth-websocket-gateway`.

El Gateway no reemplaza AS, TGS ni Service. No expone secretos, claves, tickets,
ciphertexts ni payloads internos al cliente.

## URL Local

```text
ws://127.0.0.1:2800
```

`ws://` es solo para desarrollo local. En cloud se debe publicar `wss://`.

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

Resultado exitoso:

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

Resultado fallido:

```json
{"type":"FLOW_RESULT","requestId":"front-req-2","success":false,"errorType":"SERVICE_NOT_FOUND","serviceMessage":"TGS_UNKNOWN_SERVICE"}
```

`FLOW_RESULT.success=true` no es autorizacion final. Solo indica que el Gateway
creo una sesion opaca server-side despues de un flujo exitoso.

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

1. Abrir WebSocket al Gateway.
2. Enviar `START_AUTH_FLOW`.
3. Renderizar `FLOW_EVENT` como progreso.
4. Si `FLOW_RESULT.success === true`, guardar `sessionId` solo en memoria.
5. Enviar `VERIFY_SESSION`.
6. Abrir la zona protegida solo si llega `SESSION_VALID`.
7. En logout, enviar `LOGOUT_SESSION` y limpiar estado local.

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

`SESSION_INVALID.reason` usa `NOT_FOUND`, `EXPIRED`, `REVOKED`,
`CLIENT_MISMATCH` o `SERVICE_MISMATCH`.

## Security

El frontend no debe recibir ni pedir secretos demo, claves de sesion, tickets
completos, `CryptoEnvelope`, ciphertexts ni payloads internos. La sesion opaca
puede mostrarse parcialmente enmascarada para depuracion local, pero no debe
tratarse como un token auto-verificable: la autoridad es el Gateway.
