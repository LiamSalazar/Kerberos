# Using Auth API

Esta guia muestra como una aplicacion propia puede usar este proyecto como capa
local de autenticacion modular. No es MIT Kerberos oficial y no esta listo para
produccion critica.

## Integration Model

La aplicacion externa no se conecta a SQLite. SQLite pertenece al sistema de
autenticacion y lo usan AS, TGS, Service y el WebSocket Gateway para clientes,
servicios, secretos demo y auditoria.

La aplicacion externa debe integrarse de una de estas formas:

- hablar con `auth-websocket-gateway`;
- usar `auth-client-sdk` desde una app Java controlada por el integrador.

Para aplicaciones web o frontends locales, la ruta recomendada es el Gateway
WebSocket.

## How to secure your own app with this project

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

5. Enviar `START_AUTH_FLOW` desde la app:

```json
{"type":"START_AUTH_FLOW","requestId":"app-req-1","clientId":"app-client","serviceId":"app-service"}
```

6. Validar `FLOW_RESULT`:

```js
if (message.type === "FLOW_RESULT" && message.success === true) {
  grantAccess(message.requestId);
} else {
  denyAccess(message.errorType || "FLOW_FAILED");
}
```

7. Conceder acceso al recurso:

La app crea su propia sesion o marca su propio estado autenticado solo despues
de `FLOW_RESULT.success === true`. No debe aceptar `FLOW_EVENT` como prueba de
autenticacion.

8. Consultar auditoria:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit list --limit 20
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-request --request-id app-req-1
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-client --client-id app-client
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-service --service-id app-service
```

9. Limitaciones de seguridad:

- no hay TLS/mTLS integrado;
- no hay vault externo;
- los secretos demo no deben usarse fuera de pruebas locales;
- el replay cache es local por proceso;
- la app externa sigue a cargo de UI, sesion web, logout, roles, permisos y TLS;
- Docker local, si se usa, es reproducible para demo, no despliegue productivo.

## WebSocket Contract

Mensajes de entrada:

- `START_AUTH_FLOW`
- `PING`

Mensajes de salida:

- `FLOW_EVENT`
- `FLOW_RESULT`
- `ERROR`
- `PONG`

`START_AUTH_FLOW` requiere:

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

El Gateway no expone claves, secretos, tickets, ciphertexts ni payloads
criptograficos completos.
