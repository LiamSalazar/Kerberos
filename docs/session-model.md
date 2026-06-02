# Opaque Session Model

Phase 18 closes an important Gateway gap: an external app must not treat
`FLOW_RESULT.success=true` as final authorization. That field signals that the
AS -> TGS -> Service flow completed successfully, but it can be manipulated if
the app uses it as definitive local state.

## Decision

The Gateway issues an opaque, random, server-side verifiable session. This phase
does not use RSA, JWT, or a certificate authority.

The opaque session:

- contains no secrets;
- contains no tickets;
- contains no ciphertexts;
- contains no sensitive cryptographic material;
- lives in memory, SQLite, or PostgreSQL inside the authentication system;
- is associated with `requestId`, `clientId`, and `serviceId`;
- expires;
- can be revoked with `LOGOUT_SESSION`.

## Flow

1. The app sends `START_AUTH_FLOW`.
2. The Gateway runs AS -> TGS -> Service.
3. If the service grants access, the Gateway stores a server-side session.
4. `FLOW_RESULT` returns `sessionId` and `sessionExpiresAt`.
5. The app sends `VERIFY_SESSION`.
6. Only `SESSION_VALID` allows the app's own resource to open.
7. On logout, the app sends `LOGOUT_SESSION`.

## Verification

`VERIFY_SESSION` requires:

```json
{
  "type": "VERIFY_SESSION",
  "requestId": "verify-1",
  "sessionId": "opaque-session-id",
  "clientId": "app-client",
  "serviceId": "app-service"
}
```

The Gateway responds with `SESSION_VALID` if the session exists, is still
active, has not expired, and matches the same client and service. Otherwise, it
responds with `SESSION_INVALID` and a reason:

- `NOT_FOUND`
- `EXPIRED`
- `REVOKED`
- `CLIENT_MISMATCH`
- `SERVICE_MISMATCH`

## Expiration

Expiration follows the temporal principle of the flow: the Gateway session must
not last longer than the service response. When `ServiceResponse.expiresAt` is
available, the Gateway uses the lowest value among:

- `ServiceResponse.expiresAt`;
- `AUTH_SESSION_TTL_SECONDS`;
- `AUTH_SESSION_MAX_TTL_SECONDS`.

If a future evolution could not propagate `expiresAt`, the operational limit
would be `AUTH_SESSION_MAX_TTL_SECONDS` and must be documented.

## Storage

`AUTH_SESSION_STORAGE_MODE=memory|sqlite|postgres` controls where the session
lives. If it is not configured, it follows `AUTH_STORAGE_MODE`; therefore
`sqlite` persists sessions in `auth_sessions` when the local stack uses SQLite,
and `postgres` stores them in a shared database suitable for multiple Gateway
instances.

SQLite remains internal to the authentication system. An external app must not
open that database to validate sessions; it must use `VERIFY_SESSION`.

In cloud, `memory` does not work for multiple replicas and SQLite is not
recommended as shared storage. Use PostgreSQL/RDS so `VERIFY_SESSION` and
`LOGOUT_SESSION` are coherent across Gateway instances.

## Limits

This model does not replace TLS, WSS, role policy, business permissions,
CSRF/cookie protection, or production secrets management. `ws://` is only for
local development; in cloud, use `wss://` in front of the Gateway.
