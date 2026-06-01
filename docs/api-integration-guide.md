# API Integration Guide

Esta guia explica como una app externa integra el sistema. La app no se conecta
a PostgreSQL, AS, TGS ni Service. Solo se conecta al WebSocket Gateway.

## Endpoints

Local:

```text
ws://localhost:2800
```

Cloud futuro:

```text
wss://auth.dominio.com
```

## Mensajes

### START_AUTH_FLOW

```json
{"type":"START_AUTH_FLOW","requestId":"app-1","clientId":"1","serviceId":"1"}
```

### FLOW_RESULT

Exitoso:

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

Fallido:

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

## Regla De Acceso

- Conceder acceso solo con `SESSION_VALID`.
- Negar acceso con `SESSION_INVALID`.
- Negar acceso con `ERROR`.
- Negar acceso con `FLOW_RESULT.success=false`.
- No conceder acceso solo por `FLOW_RESULT.success=true`.

## JavaScript Vanilla

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

## Backend Node Conceptual

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

## Backend Java Conceptual

```java
// Use any standard Java WebSocket client.
// Send START_AUTH_FLOW, wait for FLOW_RESULT, then send VERIFY_SESSION.
// Only map the user/app request to "authenticated" when SESSION_VALID arrives.
```

## Registrar clientId Y serviceId

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

No use secretos reales en ejemplos versionados. En cloud, use Secrets Manager.

## Errores Comunes

| Error | Manejo recomendado |
| --- | --- |
| `CLIENT_NOT_FOUND` | Negar acceso y revisar registro del clientId. |
| `SERVICE_NOT_FOUND` | Negar acceso y revisar registro del serviceId. |
| `SESSION_REQUIRED` | Negar acceso; falta `sessionId`. |
| `SESSION_EXPIRED` | Pedir nuevo login. |
| `SESSION_REVOKED` | Mantener logout. |
| `SESSION_CLIENT_MISMATCH` | Negar acceso y revisar integracion. |
| `SESSION_SERVICE_MISMATCH` | Negar acceso y revisar integracion. |
| `RATE_LIMITED` | Reintentar con backoff. |

## MelodyFinder

`sample-login-app` demuestra la integracion visual: MelodyFinder se conecta solo
al Gateway, muestra eventos resumidos y abre su dashboard protegido solo despues
de `SESSION_VALID`.
