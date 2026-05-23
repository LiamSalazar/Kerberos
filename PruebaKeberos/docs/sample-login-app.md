# Sample Login App

`sample-login-app` es una mini app HTML/CSS/JS vanilla para integradores. No usa
React, Vite, TypeScript, npm ni dependencias externas.

La app solo habla con `auth-websocket-gateway`. No abre SQLite, no importa
codigo backend y no administra clientes o servicios.

## Flow

1. La app envia `START_AUTH_FLOW`.
2. Si recibe `FLOW_RESULT success=true` con `sessionId`, guarda la sesion solo en
   memoria del navegador.
3. La app envia `VERIFY_SESSION`.
4. Solo si recibe `SESSION_VALID`, muestra el dashboard protegido.
5. Si recibe `SESSION_INVALID` o `ERROR`, mantiene el acceso cerrado.
6. En logout, envia `LOGOUT_SESSION` y limpia estado local.

Esto evita que una manipulacion local de `success=true` desbloquee la UI sin una
validacion server-side del Gateway.

## Run Backend

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

## Run App

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

## Expected Success

Usar:

```text
Gateway URL: ws://127.0.0.1:2800
clientId: 1
serviceId: 1
```

Resultado:

- `FLOW_RESULT success=true`;
- `sessionId` enmascarado en UI;
- `VERIFY_SESSION` enviado automaticamente;
- dashboard solo visible despues de `SESSION_VALID`;
- `LOGOUT_SESSION` enviado en logout;
- sin secretos, tickets, claves ni ciphertexts.

## Expected Failure

Usar `serviceId=missing-service`.

Resultado:

- `FLOW_RESULT success=false` o `ERROR`;
- dashboard oculto;
- sin `sessionId` valido;
- sin desbloqueo por estado local.

## Docker

```bash
cp .env.example .env
docker compose up -d --build
```

Abrir `http://localhost:5174`. La app consume `ws://localhost:2800` en local.
