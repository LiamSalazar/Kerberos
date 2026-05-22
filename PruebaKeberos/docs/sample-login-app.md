# Sample Login App

`sample-login-app` es una mini aplicacion HTML/CSS/JS vanilla que simula como
una app real puede usar `auth-websocket-gateway` como capa de autenticacion
distribuida local.

No usa React, Vite, TypeScript, npm ni dependencias externas. Es distinta de
`auth-web-demo`: la demo tecnica muestra etapas AS -> TGS -> Service; esta mini
app muestra una pantalla de login y una zona protegida.

## Ejecutar Backend

Windows:

```cmd
scripts\run-as.bat
scripts\run-tgs.bat
scripts\run-service.bat
scripts\run-websocket-gateway.bat
```

Linux/macOS:

```bash
scripts/run-as.sh
scripts/run-tgs.sh
scripts/run-service.sh
scripts/run-websocket-gateway.sh
```

El Gateway escucha por defecto en:

```text
ws://127.0.0.1:2800
```

## Ejecutar Con SQLite

```cmd
scripts\init-sqlite-demo.bat --db data\auth-demo.sqlite
set AUTH_STORAGE_MODE=sqlite
set AUTH_SQLITE_PATH=data\auth-demo.sqlite
```

Usa esas variables al levantar AS, TGS, Service y Gateway. En modo SQLite, el
Gateway registra eventos en `auth_audit_events`.

## Ejecutar Sample App

Windows:

```cmd
scripts\run-sample-login-app.bat
```

Linux/macOS:

```bash
scripts/run-sample-login-app.sh
```

Abrir:

```text
http://127.0.0.1:5174
```

El puerto puede cambiar con:

```text
AUTH_SAMPLE_LOGIN_PORT=5175
```

## Login Exitoso

Usar:

```text
Gateway URL: ws://127.0.0.1:2800
clientId: 1
serviceId: 1
```

Resultado esperado:

- `FLOW_RESULT success=true`;
- pantalla protegida `Welcome to MelodyFinder`;
- `requestId` visible;
- estado autenticado visible;
- mensaje del servicio visible;
- sin claves, tickets ni ciphertexts.

## Login Fallido

Usar un `serviceId` inexistente, por ejemplo:

```text
serviceId: missing-service
```

Resultado esperado:

- `FLOW_RESULT success=false`;
- error claro en la pantalla de login;
- zona protegida oculta;
- sin secretos ni payloads sensibles.

## Adaptar A Una App Real

1. Registrar el cliente real con `scripts\sqlite-admin.bat ... clients add`.
2. Registrar el servicio real con `scripts\sqlite-admin.bat ... services add`.
3. Levantar AS/TGS/Service/Gateway en modo SQLite.
4. En la app, abrir WebSocket al Gateway.
5. Enviar `START_AUTH_FLOW` con `requestId`, `clientId` y `serviceId`.
6. Conceder acceso solo si `FLOW_RESULT.success === true`.
7. Guardar una sesion propia de la app si corresponde.
8. Consultar auditoria con `scripts\sqlite-admin.bat ... audit list`.

La app real sigue siendo responsable de TLS, sesiones web, logout, autorizacion
de negocio, expiracion de sesion y manejo de usuarios.

## Validacion Sin npm

La app se valida sirviendo archivos estaticos con Python via
`scripts\run-sample-login-app.bat` o `scripts/run-sample-login-app.sh`. No hay
`package.json`, build step ni dependencias npm.
