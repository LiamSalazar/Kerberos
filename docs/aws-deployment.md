# AWS Deployment Readiness

Phase 20 prepares the path for a production-like deployment on AWS, without
deploying real resources and without using credentials. This documentation
describes the recommended architecture to validate after Docker works on Linux
and before operating a real AWS account.

## Recommended Architecture

Public entry:

- Public Application Load Balancer.
- HTTPS `443` listener with ACM certificate.
- WSS to `auth-websocket-gateway` on ECS Fargate.
- `auth-web-demo` and `sample-login-app` as public containers behind the same
  ALB, ideally through separate hostnames.

Private zone:

- `auth-as` on ECS Fargate without a published port.
- `auth-tgs` on ECS Fargate without a published port.
- `auth-service` on ECS Fargate without a published port.
- Private database, preferably RDS PostgreSQL.
- Secrets Manager for operational secrets.
- CloudWatch Logs for every container.
- Private HTTP health for AS/TGS/Service and Gateway `/health` for the ALB.

## What Is Public

- ALB.
- Gateway WSS.
- Frontend demo.
- Sample login app.

The Gateway is the only point that should talk to AS/TGS/Service. Browser apps
must not open connections to the database or private services.

## What Is Private

- AS.
- TGS.
- Service.
- Database.
- Secrets.
- Internal traffic between Gateway and authentication services.

## WSS With ACM And ALB

AWS uses `wss://` with TLS terminated at the ALB through ACM. The ALB routes to
the Gateway target group on port `2800`; the container keeps listening with
`AUTH_WS_HOST=0.0.0.0` and `AUTH_WS_PORT=2800`.

Before real deployment, validate that the ALB uses `path=/health` and
`port=2801`. User WSS traffic still enters through `443` and routes to Gateway
port `2800`.

`acm_certificate_arn` must point to a real ACM certificate. The domain is
configured with blueprint host headers. Route 53 is optional: if used, create
records toward the ALB DNS for Gateway, demo, and login.

## Variables

Expected Gateway variables:

```text
AUTH_MODE=strict
AUTH_STORAGE_MODE=postgres
AUTH_SESSION_STORAGE_MODE=postgres
AUTH_REQUIRE_SESSION_VERIFY=true
AUTH_SECRET_PROVIDER=aws-secrets-manager
AUTH_AWS_REGION=us-east-1
AUTH_SECRET_CLIENT_SECRET_ID=<secret-arn>
AUTH_SECRET_TGS_SECRET_ID=<secret-arn>
AUTH_SECRET_SERVICE_SECRET_ID=<secret-arn>
AUTH_SECRET_POSTGRES_PASSWORD_ID=<secret-arn>
AUTH_POSTGRES_URL=jdbc:postgresql://<rds-endpoint>:5432/kerberos_auth
AUTH_POSTGRES_USER=<user>
AUTH_POSTGRES_SSL_MODE=require
AUTH_SESSION_TTL_SECONDS=300
AUTH_SESSION_MAX_TTL_SECONDS=900
AUTH_WS_HOST=0.0.0.0
AUTH_WS_PORT=2800
AUTH_HEALTH_HOST=0.0.0.0
AUTH_HEALTH_PORT=2801
AUTH_ALLOWED_ORIGINS=https://demo.example.com,https://login.example.com
```

AS/TGS/Service must receive internal hosts and ports through Cloud Map or
equivalent variables. Secrets must come from Secrets Manager, not from committed
files.

## SQLite Is Not Production

SQLite serves reproducible local validation and persistent audit on a single
node. It is not the right option for a distributed ECS topology because:

- it does not provide suitable multi-instance concurrency for shared writes;
- it does not solve high availability or managed replicas;
- it complicates backups, managed encryption, and operational rotation;
- it does not fit well with a horizontally scaled Gateway.

## Migration To RDS PostgreSQL

The recommended path uses `auth-storage-postgres`, keeps the `auth-core`
repository contracts, runs versioned migrations, and enables
`AUTH_STORAGE_MODE=postgres`. RDS must live in private subnets, with a Security
Group that allows `5432` only from ECS.

## Secrets Manager

Secrets Manager must store:

- demo client secret or real client secrets;
- TGS secret;
- Service secret;
- PostgreSQL password or sensitive connection parts;
- any future operational encryption value.

Do not create secret versions with real values in this repository. In AWS, ECS
tasks must resolve secrets by ARN with the task role and least-privilege
permissions.

## Logs

Each service must write to CloudWatch Logs:

- `/ecs/<prefix>/auth-as`
- `/ecs/<prefix>/auth-tgs`
- `/ecs/<prefix>/auth-service`
- `/ecs/<prefix>/auth-websocket-gateway`
- `/ecs/<prefix>/auth-web-demo`
- `/ecs/<prefix>/sample-login-app`

Review startup errors, session rejections, allowed-origin failures, and AS ->
TGS -> Service flow latency.

## Scaling Gateway

The Gateway can scale horizontally only if opaque sessions are stored in a
distributed layer. With local SQLite, two Gateway tasks do not share state. AWS
requires PostgreSQL, Redis, or another server-side shared layer with coherent
expiration and logout.

## Status

This phase leaves the project at cloud-production readiness after validation.
It does not make it truly production-ready or apply infrastructure.
