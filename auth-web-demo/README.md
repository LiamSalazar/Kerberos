# Auth Web Demo

Local frontend demo to observe the modular AS -> TGS -> Service flow through
`auth-websocket-gateway`.

It does not use React, Vite, TypeScript, a bundler, or external npm
dependencies. The `npm run dev` and `npm run build` commands use custom Node.js
scripts.

Visual name from Phase 23A: **Kerberos System Demonstration**. The main screen
is **Kerberos Auth Control Center**.

## Requirements

- Node.js 18 or higher.
- npm.
- Modular backend running: AS, TGS, Service, and WebSocket Gateway.

## Install

```bash
npm install
```

This does not install external packages; it only creates local npm metadata.

## Run

```bash
npm run dev
```

Default:

```text
http://127.0.0.1:5173
```

## Validate

```bash
npm run build
```

The build validates static files and copies the demo to `dist/`.

## Manual Flow

1. Start `auth-as`, `auth-tgs`, and `auth-service`.
2. Start `auth-websocket-gateway`.
3. Open the local web demo.
4. Keep `ws://127.0.0.1:2800`.
5. Press `Connect`.
6. Press `Start Auth Flow`.
7. Verify `FLOW_*` events, `FLOW_RESULT success`, masked `sessionId`, and
   `sessionExpiresAt`.
8. Use `Verify Session` to send `VERIFY_SESSION`.
9. Use `Logout Session` to send `LOGOUT_SESSION`.

## Visualization

The UI shows:

- connection, storage/mode when available, latency, and session chips;
- map Client App -> WebSocket Gateway -> AS -> TGS -> Service -> Opaque Session;
- conceptual messages without sensitive material;
- Live Protocol Trace;
- Access Decision;
- Security Explanation;
- Security Validation Lab.

It does not show secrets, keys, full tickets, ciphertexts, or sensitive
cryptographic material. `FLOW_RESULT.success=true` must not be used as final
authorization in a real app; `SESSION_VALID` is required.
