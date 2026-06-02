# Integration API

This document summarizes how to connect real applications to the modular
runtime without coupling them to the internal database used by memory, SQLite,
or PostgreSQL modes.

Storage belongs to the authentication system. An external app must not open or
query SQLite/PostgreSQL directly; it must use `auth-websocket-gateway` or
`auth-client-sdk` in controlled Java integrations.

## Responsibilities

The authentication system:

- runs AS -> TGS -> Service;
- validates tickets, authenticators, expiration, clock skew, and local replay;
- issues verifiable opaque sessions in the Gateway;
- records persistent audit events when running with `AUTH_STORAGE_MODE=sqlite` or
  `AUTH_STORAGE_MODE=postgres`.

The integrating app:

- handles UI and user experience;
- calls `START_AUTH_FLOW`;
- calls `VERIFY_SESSION` before opening resources;
- calls `LOGOUT_SESSION` when closing the session;
- maintains roles, permissions, and business authorization;
- uses TLS/WSS outside local environments.

## WebSocket Contract

Input:

- `START_AUTH_FLOW`
- `VERIFY_SESSION`
- `LOGOUT_SESSION`
- `PING`

Output:

- `FLOW_EVENT`
- `FLOW_RESULT`
- `SESSION_VALID`
- `SESSION_INVALID`
- `SESSION_LOGGED_OUT`
- `ERROR`
- `PONG`

`FLOW_RESULT.success=true` is not final authorization. It only means the Gateway
could create a server-side session after a successful flow.

## How To Secure Your Own App With This Project

1. Register the client in the selected internal storage.
2. Register the service in the selected internal storage.
3. Configure `AUTH_STORAGE_MODE=sqlite` for local or
   `AUTH_STORAGE_MODE=postgres` for cloud-like/RDS.
4. Start AS, TGS, Service, and WebSocket Gateway.
5. Send `START_AUTH_FLOW`.
6. If `FLOW_RESULT.success === true`, store `sessionId` only in memory.
7. Send `VERIFY_SESSION` with `sessionId`, `clientId`, and `serviceId`.
8. Grant access only after `SESSION_VALID`.
9. On logout, send `LOGOUT_SESSION`.
10. Query audit events with `audit list`, `by-request`, `by-client`, or
    `by-service`.
11. Document limitations: no built-in TLS/mTLS, no external vault, and no
    critical production guarantees.

## Protected Resource

`auth-service` defines:

```java
String getServiceId();
ProtectedServiceResponse execute(ProtectedServiceRequest request);
```

`ProtectedServiceHandler` validates the protocol before invoking the resource.
A real service can live behind `auth-service`; it does not need to connect to
internal storage directly.

Current implementations:

- `DemoProtectedResource`: local demonstration response.
- `HttpProtectedResource`: example against a local HTTP server in tests.

## Notes For Cloud

`ws://` is only for local development. In cloud, publish `wss://` with TLS
termination, operational limits, logs without secrets, and real secrets
management. RSA/JWT/CA remain possible evolutions, not requirements for this
phase.
