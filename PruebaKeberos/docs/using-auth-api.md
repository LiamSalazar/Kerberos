# Using Auth API

Esta guia muestra como una app propia puede usar el Gateway como capa local de
autenticacion modular. No es MIT Kerberos oficial y no esta listo para
produccion critica.

## Integration Model

La app externa no se conecta a SQLite. SQLite guarda clientes, servicios,
secretos demo, auditoria y sesiones del sistema de autenticacion. La app debe
hablar con el Gateway o con `auth-client-sdk`.

## How To Secure Your Own App With This Project

1. Registrar cliente:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients add --id app-client --display-name "App Client" --secret "<demo-secret>"
```

2. Registrar servicio:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services add --id app-service --display-name "App Service" --secret "<demo-secret>" --endpoint local://app-service
```

3. Configurar SQLite:

```cmd
set AUTH_STORAGE_MODE=sqlite
set AUTH_SQLITE_PATH=data\auth-demo.sqlite
```

4. Levantar sistema:

```cmd
scripts\run-as.bat
scripts\run-tgs.bat
scripts\run-service.bat
scripts\run-websocket-gateway.bat
```

5. Enviar `START_AUTH_FLOW`:

```json
{"type":"START_AUTH_FLOW","requestId":"app-req-1","clientId":"app-client","serviceId":"app-service"}
```

6. Leer `FLOW_RESULT`:

```js
if (message.type === "FLOW_RESULT" && message.success === true && message.sessionId) {
  pendingSessionId = message.sessionId;
  verifySession(pendingSessionId);
}
```

7. Validar con `VERIFY_SESSION`:

```json
{"type":"VERIFY_SESSION","requestId":"verify-1","sessionId":"opaque-session-id","clientId":"app-client","serviceId":"app-service"}
```

8. Conceder acceso solo con `SESSION_VALID`:

```js
if (message.type === "SESSION_VALID" && message.valid === true) {
  grantAccess(message.clientId, message.serviceId);
}
```

9. Cerrar sesion:

```json
{"type":"LOGOUT_SESSION","requestId":"logout-1","sessionId":"opaque-session-id"}
```

10. Consultar auditoria:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit list --limit 20
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-request --request-id app-req-1
```

## Security Limits

- `FLOW_RESULT.success=true` no es autorizacion final.
- La autoridad practica de sesion es el Gateway.
- La sesion opaca no debe guardarse en `localStorage` para esta demo.
- `ws://` es solo local; en cloud usar `wss://`.
- No hay RSA, JWT ni CA en esta fase.
- La app externa sigue a cargo de roles, permisos, cookies, CSRF, TLS y negocio.
