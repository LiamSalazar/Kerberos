# Frontend Demo

`auth-web-demo` is the local technical demo for observing the modular
AS -> TGS -> Service flow through `auth-websocket-gateway`.

It is not the login app. For the integration example with a protected area, use
`sample-login-app`.

## What It Shows

- header `Kerberos Auth Control Center`;
- Gateway, storage/mode when available, latency, and session chips;
- connection panel with `START_AUTH_FLOW`, `VERIFY_SESSION`, `LOGOUT_SESSION`,
  and `Clear Trace`;
- map Client App -> WebSocket Gateway -> AS -> TGS -> Service -> Opaque Session;
- conceptual message cards;
- Live Protocol Trace;
- Access Decision;
- Security Explanation;
- Security Validation Lab;
- protocol errors without showing secrets.

## Run

Windows:

```cmd
scripts\run-as.bat
scripts\run-tgs.bat
scripts\run-service.bat
scripts\run-websocket-gateway.bat
scripts\run-web-demo.bat
```

Linux/macOS:

```bash
scripts/run-as.sh
scripts/run-tgs.sh
scripts/run-service.sh
scripts/run-websocket-gateway.sh
scripts/run-web-demo.sh
```

Open:

```text
http://127.0.0.1:5173
```

## Build Validation

```bash
cd auth-web-demo
npm install
npm run build
```

`auth-web-demo/dist/` is generated and must not be versioned.

## Security Display

The UI shows only state, stages, high-level messages, latencies, and masked
opaque session. It does not render secrets, full tickets, keys, ciphertexts,
`CryptoEnvelope`, or internal payloads.

`FLOW_RESULT.success=true` must not be interpreted as final authorization in a
real app. Practical authorization requires `VERIFY_SESSION` and `SESSION_VALID`.
