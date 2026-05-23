# Kerberos-Inspired Modular Authentication Demo

This repository contains a Java portfolio project under `PruebaKeberos/`. It
implements a Kerberos-inspired modular authentication flow with AS, TGS, Service,
SQLite-backed local storage, a WebSocket Gateway, vanilla frontend demos, and a
Docker Compose setup for local reproducible testing.

It is not official MIT Kerberos and it is not production-ready. The project is
intended for local study, integration experiments, and controlled demos.

## Current Focus

The active line is the modular `auth-*` runtime:

- `auth-core`: shared DTOs, configuration, audit and session contracts.
- `auth-crypto`: AES-GCM and local cryptographic envelopes.
- `auth-transport`: JSON/TCP protocol transport.
- `auth-as`, `auth-tgs`, `auth-service`: modular authentication services.
- `auth-client-sdk`: Java client and audit runners.
- `auth-storage-sqlite`: local SQLite repositories, migrations and CLI.
- `auth-websocket-gateway`: WebSocket API for external apps and demos.
- `auth-web-demo`: technical browser demo for the auth flow.
- `sample-login-app`: vanilla sample login app for integrators.

## Quick Start Without Docker

```bash
cd PruebaKeberos
mvn -q -DskipTests compile
mvn test
```

Run the local services in separate terminals:

```bash
scripts/run-as.sh
scripts/run-tgs.sh
scripts/run-service.sh
scripts/run-websocket-gateway.sh
scripts/run-web-demo.sh
scripts/run-sample-login-app.sh
```

On Windows, use the matching `.bat` scripts from `PruebaKeberos/scripts/`.

## Quick Start With Docker

Docker is optional and currently scoped to local reproducible validation:

```bash
cd PruebaKeberos
cp .env.example .env
docker compose config
docker compose build
docker compose up
```

Open:

- `http://localhost:5173` for the technical web demo.
- `http://localhost:5174` for the sample login app.
- `ws://localhost:2800` for the WebSocket Gateway.

AS, TGS and Service stay inside the internal Docker network. Only the Gateway
and frontend demos are published to the host.

## Integration Rule

External apps must not connect directly to the SQLite database. SQLite belongs
to the authentication system. Apps should use `auth-websocket-gateway` or the
Java `auth-client-sdk`.

For browser-style integrations, the app sends `START_AUTH_FLOW` and then
verifies the returned opaque session with `VERIFY_SESSION`. A frontend must not
treat `FLOW_RESULT.success=true` as the final access authority by itself.

## Important Limits

- No RSA, JWT or certificate authority is implemented in the current design.
- No Spring Boot, PostgreSQL or heavy ORM is used.
- Demo secrets are only for local execution.
- Plain `ws://` is only for local development; cloud deployments need `wss://`
  and proper operational hardening.

See [PruebaKeberos/README.md](PruebaKeberos/README.md) and the documentation
under [PruebaKeberos/docs](PruebaKeberos/docs) for the full execution and
integration guide.
