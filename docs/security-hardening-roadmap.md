# Security Hardening Roadmap

This project is a portfolio piece inspired by Kerberos 4. It is not official
MIT Kerberos and is not ready for critical production use.

## Current Status

The modular path is the main path:

- uses DTOs and `ProtocolEnvelope`;
- transports JSON over TCP;
- encrypts tickets, authenticators, and responses with AES-GCM;
- transports IV/nonce and ciphertext in `CryptoEnvelope`;
- uses stable associated data by object type;
- blocks repeated `requestId` values and authenticators with `InMemoryReplayCache`;
- validates expiration and clock skew in TGS and Service;
- has timeouts and message-size limits in TCP transport;
- has `AUTH_MODE=strict` to reject demo secrets;
- includes reproducible execution/performance audit;
- includes concurrency test and audit with versioned evidence;
- supports `AUTH_STORAGE_MODE=memory|sqlite|postgres`; SQLite is local and
  PostgreSQL is prepared for RDS/cloud;
- includes versioned SQLite migrations and `schema_version`;
- records local persistent audit of WebSocket flows in SQLite when
  `AUTH_STORAGE_MODE=sqlite` is used;
- adds local CLI for registering/listing/enabling/disabling clients and services;
- adds `auth-websocket-gateway` as a separate layer for future web integrations,
  without replacing the modular TCP path;
- adds `auth-web-demo` as a decoupled local frontend that consumes only the
  WebSocket Gateway;
- adds `sample-login-app` as a vanilla mini app to demonstrate login-style
  integration;
- adds verifiable opaque sessions in the Gateway so an external app does not
  grant access only because `FLOW_RESULT.success=true`;
- adds `VERIFY_SESSION` and `LOGOUT_SESSION`, with sessions in memory, SQLite,
  or PostgreSQL depending on configuration;
- adds `SecretsProvider` for environment variables and AWS Secrets Manager;
- adds HTTP health, sanitized JSON logs, and basic Gateway metrics.

The physical legacy code has already been removed from the main project.
Phase 9 also removed `auth-transport/javaio` and `auth-transport/legacy`.
Historical context remains documented in `docs/legacy-summary.md`.

## Open Modular Risks

- Default secrets still exist in `demo` mode.
- `AUTH_MODE=strict` validates explicit secrets and can resolve Secrets Manager,
  but it does not add real rotation.
- `InMemoryReplayCache` is not shared between processes.
- Local SQLite does not replace a production persistence, secret encryption, or
  rotation strategy.
- PostgreSQL/RDS is prepared, but still needs validation against real infrastructure.
- Persistent audit records high-level events, not complete traces or distributed observability.
- `JsonMessageCodec` is a project-scoped codec, not a general-purpose audited JSON parser.
- There is no TLS or mutual transport authentication.
- Current secret names are `AUTH_DEMO_*`; `AUTH_MODE=strict` requires explicit values.
- The WebSocket Gateway has unit, component, and real E2E tests with a WebSocket
  client and modular servers started inside Maven.
- The web demo does not show secrets, full tickets, ciphertexts, or sensitive
  cryptographic material.
- `ws://` communication remains local only. Cloud requires `wss://`, TLS
  termination, and operational hardening.
- RSA, JWT, and certificate authority were not added; the current model uses an
  opaque session validated server-side by the Gateway.

## Next Priority

1. Validate Docker on Linux and controlled real PostgreSQL.
2. Harden the WebSocket/frontend channel with TLS or local authentication if
   authorized.
3. Evaluate Jackson/Gson or another maintained JSON parser if the custom codec
   grows beyond its limited scope.
4. Add TLS or an authenticated transport layer for the modular path.
5. Load real secrets in Secrets Manager and define rotation.
6. Add browser E2E tests for the web demo if tooling is authorized.
7. Evaluate TLS/WSS and secrets management before any cloud deployment.

## WebSocket Dependency

`org.java-websocket:Java-WebSocket` was added because standard Java does not
provide a simple WebSocket server for this case. The dependency remains limited
to the `auth-websocket-gateway` module and avoids introducing Spring Boot or a
full application framework.

Jackson/Gson was not added in this phase. The custom JSON codec remains because
it is still scoped to protocol DTOs and flat Gateway messages, with tests for
malformed JSON, missing fields, incorrect types, and invalid payload.

## Cloud Dependencies

Phase 20 adds `org.postgresql:postgresql` for `auth-storage-postgres` and AWS
SDK v2 `software.amazon.awssdk:secretsmanager` in `auth-core`. No ORM, Spring
Boot, RSA, JWT, or certificate authority was added.

## SQLite Dependency

`org.xerial:sqlite-jdbc` was added in `auth-storage-sqlite` to test local
persistence without an external server and without an ORM. The dependency is
not used to store production secrets; it only enables a verifiable local
integration with schema and demo seed.
