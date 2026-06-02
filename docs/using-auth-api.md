# Using Auth API

This guide shows how your own app can use the Gateway as a local modular
authentication layer. It is not official MIT Kerberos and is not ready for
critical production use.

## Integration Model

The external app does not connect to SQLite. SQLite stores clients, services,
demo secrets, audit events, and sessions for the authentication system. The app
must talk to the Gateway or to `auth-client-sdk`.

In cloud, the same rule applies to PostgreSQL/RDS: the external app does not
query the shared database. It must validate opaque sessions with
`VERIFY_SESSION` and accept access only with `SESSION_VALID`.

## How To Secure Your Own App With This Project

1. Register the client:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients add --id app-client --display-name "App Client" --secret "<demo-secret>"
```

2. Register the service:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services add --id app-service --display-name "App Service" --secret "<demo-secret>" --endpoint local://app-service
```

3. Configure SQLite:

```cmd
set AUTH_STORAGE_MODE=sqlite
set AUTH_SQLITE_PATH=data\auth-demo.sqlite
```

4. Start the system:

```cmd
scripts\run-as.bat
scripts\run-tgs.bat
scripts\run-service.bat
scripts\run-websocket-gateway.bat
```

5. Send `START_AUTH_FLOW`:

```json
{"type":"START_AUTH_FLOW","requestId":"app-req-1","clientId":"app-client","serviceId":"app-service"}
```

6. Read `FLOW_RESULT`:

```js
if (message.type === "FLOW_RESULT" && message.success === true && message.sessionId) {
  pendingSessionId = message.sessionId;
  verifySession(pendingSessionId);
}
```

7. Validate with `VERIFY_SESSION`:

```json
{"type":"VERIFY_SESSION","requestId":"verify-1","sessionId":"opaque-session-id","clientId":"app-client","serviceId":"app-service"}
```

8. Grant access only with `SESSION_VALID`:

```js
if (message.type === "SESSION_VALID" && message.valid === true) {
  grantAccess(message.clientId, message.serviceId);
}
```

9. Close the session:

```json
{"type":"LOGOUT_SESSION","requestId":"logout-1","sessionId":"opaque-session-id"}
```

10. Query audit events:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit list --limit 20
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-request --request-id app-req-1
```

## Security Limits

- `FLOW_RESULT.success=true` is not final authorization.
- The practical session authority is the Gateway.
- The opaque session should not be stored in `localStorage` for this demo.
- `ws://` is local only; use `wss://` in cloud.
- For multiple Gateway instances, use `AUTH_SESSION_STORAGE_MODE=postgres`.
- There is no RSA, JWT, or CA in this phase.
- The external app remains responsible for roles, permissions, cookies, CSRF, TLS, and business logic.
