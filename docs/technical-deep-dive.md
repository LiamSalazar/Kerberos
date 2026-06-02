# Technical Deep Dive

## Service Startup

- `auth-as`: runs `AuthenticationServerApp` and listens for AS requests over TCP/JSON.
- `auth-tgs`: runs `TicketGrantingServerApp` and processes TGS requests.
- `auth-service`: runs `ProtectedServiceApp` and protects the demo resource.
- `auth-websocket-gateway`: runs `WebSocketGatewayApp`, exposes WebSocket `2800` and health `2801`.
- `auth-web-demo`: serves vanilla HTML/CSS/JS on `5173`.
- `sample-login-app`: serves MelodyFinder on `5174`.

## Modules

- `auth-core`: contracts, DTOs, configuration, replay cache, sessions, health,
  structured logs, and metrics.
- `auth-crypto`: AES-GCM, `CryptoEnvelope`, derivation, and session material.
- `auth-transport`: `ProtocolEnvelope`, JSON codec, and TCP.
- `auth-as`: initial client validation.
- `auth-tgs`: conceptual issuance of service access.
- `auth-service`: final validation and protected resource.
- `auth-client-sdk`: `AuthClient`, `AuthFlowRunner`, CLI, and audits.
- `auth-storage-sqlite`: SQLite repositories, migrations, and local CLI.
- `auth-storage-postgres`: PostgreSQL/RDS-ready repositories.
- `auth-websocket-gateway`: WebSocket integration boundary.
- `auth-web-demo`: technical protocol visualization.
- `sample-login-app`: sample app for integrators.

## Docker

`docker-compose.yml` defines services for AS, TGS, Service, Gateway, frontends,
and optional PostgreSQL with the `postgres-local` profile.

Dockerfiles:

- `docker/as/Dockerfile`: packages the AS runtime.
- `docker/tgs/Dockerfile`: packages the TGS runtime.
- `docker/service/Dockerfile`: packages the Service runtime.
- `docker/gateway/Dockerfile`: packages the Gateway and runtime dependencies.
- `docker/web-demo/Dockerfile`: serves `auth-web-demo`.
- `docker/sample-login-app/Dockerfile`: serves `sample-login-app`.

## Terraform

- `infra/aws/terraform/main.tf`: VPC, ALB, ECS/Fargate, ECR, RDS, logs, and routes.
- `variables.tf`: blueprint parameters.
- `outputs.tf`: useful plan outputs.
- `terraform.tfvars.example`: example HTTP/local blueprint variables.
- `terraform.tfvars.https.example`: example HTTPS/WSS variables.

RSA/JWT are not used in this phase because the goal is to keep opaque sessions
server-side and avoid exposing self-verifiable tokens to the frontend.

PostgreSQL is the prepared path for multiple Gateway instances because
`VERIFY_SESSION` and `LOGOUT_SESSION` must query shared state. Local memory does
not work for cloud replicas.

WSS must terminate at ALB/ACM so the browser uses TLS and the Gateway receives
controlled internal traffic.

## Conceptual Messages In The UI

The UI shows names and conceptual content:

- `START_AUTH_FLOW`: clientId, serviceId, requestId.
- AS request/response: identity, requested TGS, conceptual lifetime.
- TGS request/response: conceptual ticket, authenticator, requested service.
- Service request/response: access decision and protected response.
- `FLOW_RESULT`: success/failure, opaque session, expiration, and latency.
- `VERIFY_SESSION`: opaque session and client/service context.

It does not show real cryptographic material.

## Threats And Limits

Visualized threats:

- unknown client;
- unknown service;
- invalid session;
- replay protection;
- expired session;
- sensitive data exposure.

Relevant automated validations:

- JSON codec;
- AES-GCM;
- AS -> TGS -> Service flow;
- replay cache;
- opaque sessions;
- Gateway WebSocket;
- SQLite/PostgreSQL.

Open limits:

- AWS apply was not executed;
- full production hardening was not performed;
- WSS requires a real ACM certificate;
- cloud secrets must live in Secrets Manager;
- demos are local and do not replace automated tests.
