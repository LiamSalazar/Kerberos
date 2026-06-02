# Sample Login App

`sample-login-app` is a vanilla HTML/CSS/JS mini app for integrators. It does
not use React, Vite, TypeScript, npm, or external dependencies.

Visual name from Phase 23A: **MelodyFinder**.

The app only talks to `auth-websocket-gateway`. It does not open SQLite, import
backend code, or administer clients or services.

## Flow

1. The app sends `START_AUTH_FLOW`.
2. If it receives `FLOW_RESULT success=true` with `sessionId`, it stores the
   session only in browser memory.
3. The app sends `VERIFY_SESSION`.
4. Only if it receives `SESSION_VALID` does it show the protected dashboard.
5. If it receives `SESSION_INVALID` or `ERROR`, it keeps access closed.
6. On logout, it sends `LOGOUT_SESSION` and clears local state.

This prevents local manipulation of `success=true` from unlocking the UI without
server-side validation from the Gateway.

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

Open:

```text
http://127.0.0.1:5174
```

## Expected Success

Use:

```text
Gateway URL: ws://127.0.0.1:2800
clientId: 1
serviceId: 1
```

Result:

- `FLOW_RESULT success=true`;
- masked `sessionId` in the UI;
- `VERIFY_SESSION` sent automatically;
- dashboard visible only after `SESSION_VALID`;
- `LOGOUT_SESSION` sent on logout;
- no secrets, tickets, keys, or ciphertexts.

The UI also shows a technical panel, "What is happening behind the scenes?",
with Gateway, Authentication, Session, and Access status.

## Expected Failure

Use `serviceId=missing-service`.

Result:

- `FLOW_RESULT success=false` or `ERROR`;
- dashboard hidden;
- no valid `sessionId`;
- no unlock through local state.

## Docker

```bash
cp .env.example .env
docker compose up -d --build
```

Open `http://localhost:5174`. The app consumes `ws://localhost:2800` locally.
