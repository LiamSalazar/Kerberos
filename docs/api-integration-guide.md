# API Integration Guide

This guide explains how an external app integrates with the system. The app
does not connect to PostgreSQL, AS, TGS, or Service. It only connects to the
WebSocket Gateway.

## Endpoints

Local:

```text
ws://localhost:2800
```

Future cloud:

```text
wss://auth.example.com
```

## Messages

### START_AUTH_FLOW

```json
{"type":"START_AUTH_FLOW","requestId":"app-1","clientId":"1","serviceId":"1"}
```

### FLOW_RESULT

Successful:

```json
{
  "type": "FLOW_RESULT",
  "requestId": "app-1",
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

Failed:

```json
{"type":"FLOW_RESULT","requestId":"app-2","success":false,"errorType":"SERVICE_NOT_FOUND","serviceMessage":"TGS_UNKNOWN_SERVICE"}
```

### VERIFY_SESSION

```json
{"type":"VERIFY_SESSION","requestId":"app-1-verify","sessionId":"opaque-session-id","clientId":"1","serviceId":"1"}
```

### SESSION_VALID

```json
{"type":"SESSION_VALID","requestId":"app-1-verify","valid":true,"clientId":"1","serviceId":"1","expiresAt":"2026-05-23T17:00:00Z"}
```

### SESSION_INVALID

```json
{"type":"SESSION_INVALID","requestId":"app-1-verify","valid":false,"reason":"EXPIRED"}
```

### LOGOUT_SESSION

```json
{"type":"LOGOUT_SESSION","requestId":"app-1-logout","sessionId":"opaque-session-id"}
```

### SESSION_LOGGED_OUT

```json
{"type":"SESSION_LOGGED_OUT","requestId":"app-1-logout","message":"Sesion revocada"}
```

### ERROR

```json
{"type":"ERROR","errorType":"MISSING_REQUIRED_FIELD","message":"Campo requerido faltante: serviceId","success":false}
```

## Access Rule

- Grant access only with `SESSION_VALID`.
- Deny access with `SESSION_INVALID`.
- Deny access with `ERROR`.
- Deny access with `FLOW_RESULT.success=false`.
- Do not grant access only because `FLOW_RESULT.success=true`.

## Vanilla JavaScript

```js
const socket = new WebSocket("ws://localhost:2800");
const requestId = `app-${Date.now()}`;
let sessionId = null;

socket.addEventListener("open", () => {
  socket.send(JSON.stringify({
    type: "START_AUTH_FLOW",
    requestId,
    clientId: "1",
    serviceId: "1"
  }));
});

socket.addEventListener("message", (event) => {
  const message = JSON.parse(event.data);

  if (message.type === "FLOW_RESULT" && message.success === true) {
    sessionId = message.sessionId;
    socket.send(JSON.stringify({
      type: "VERIFY_SESSION",
      requestId: `${requestId}-verify`,
      sessionId,
      clientId: "1",
      serviceId: "1"
    }));
    return;
  }

  if (message.type === "SESSION_VALID") {
    grantAccess();
    return;
  }

  if (message.type === "SESSION_INVALID" || message.type === "ERROR") {
    denyAccess(message.reason || message.errorType || "DENIED");
  }
});
```

## Conceptual Node Backend

```js
import WebSocket from "ws";

export function authenticateWithGateway(clientId, serviceId) {
  return new Promise((resolve, reject) => {
    const socket = new WebSocket("ws://localhost:2800");
    const requestId = `backend-${Date.now()}`;
    let sessionId;

    socket.on("open", () => {
      socket.send(JSON.stringify({ type: "START_AUTH_FLOW", requestId, clientId, serviceId }));
    });

    socket.on("message", (raw) => {
      const message = JSON.parse(raw);
      if (message.type === "FLOW_RESULT" && message.success) {
        sessionId = message.sessionId;
        socket.send(JSON.stringify({
          type: "VERIFY_SESSION",
          requestId: `${requestId}-verify`,
          sessionId,
          clientId,
          serviceId
        }));
      } else if (message.type === "SESSION_VALID") {
        resolve({ sessionId, expiresAt: message.expiresAt });
        socket.close();
      } else if (message.type === "SESSION_INVALID" || message.type === "ERROR") {
        reject(new Error(message.reason || message.errorType || "AUTH_DENIED"));
        socket.close();
      }
    });
  });
}
```

## Conceptual Java Backend

```java
// Use any standard Java WebSocket client.
// Send START_AUTH_FLOW, wait for FLOW_RESULT, then send VERIFY_SESSION.
// Only map the user/app request to "authenticated" when SESSION_VALID arrives.
```

## Register clientId And serviceId

SQLite demo:

```powershell
scripts\init-sqlite-demo.bat --db data\auth-demo.sqlite
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients add --id app-client --display-name "App Client" --secret "<demo-secret>"
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services add --id app-service --display-name "App Service" --secret "<demo-secret>" --endpoint local://app-service
```

Linux/macOS:

```bash
scripts/init-sqlite-demo.sh --db data/auth-demo.sqlite
scripts/sqlite-admin.sh --db data/auth-demo.sqlite clients add --id app-client --display-name "App Client" --secret "<demo-secret>"
scripts/sqlite-admin.sh --db data/auth-demo.sqlite services add --id app-service --display-name "App Service" --secret "<demo-secret>" --endpoint local://app-service
```

Do not use real secrets in versioned examples. In cloud, use Secrets Manager.

## Common Errors

| Error | Recommended handling |
| --- | --- |
| `CLIENT_NOT_FOUND` | Deny access and review the clientId registration. |
| `SERVICE_NOT_FOUND` | Deny access and review the serviceId registration. |
| `SESSION_REQUIRED` | Deny access; `sessionId` is missing. |
| `SESSION_EXPIRED` | Request a new login. |
| `SESSION_REVOKED` | Keep logout state. |
| `SESSION_CLIENT_MISMATCH` | Deny access and review the integration. |
| `SESSION_SERVICE_MISMATCH` | Deny access and review the integration. |
| `RATE_LIMITED` | Retry with backoff. |

## MelodyFinder

`sample-login-app` demonstrates the visual integration: MelodyFinder connects
only to the Gateway, shows summarized events, and opens its protected dashboard
only after `SESSION_VALID`.
