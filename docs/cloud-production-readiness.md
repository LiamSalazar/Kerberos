# Cloud Production Readiness

This project must not be presented as enterprise production-ready. Phase 20
prepares a cloud-production readiness path and documents what is still missing
to harden it.

## Already Prepared

- Real monorepo at the root.
- Maven executable from the root.
- Docker Compose ready for Linux validation.
- WebSocket Gateway with verifiable opaque sessions.
- Local persistent SQLite audit.
- `auth-storage-postgres` module with JDBC repositories and SQL migrations.
- `SecretsProvider` with `env` and prepared AWS Secrets Manager support.
- Lightweight HTTP health and `/metrics` endpoint for Java processes.
- Sanitized JSON logs in the Gateway for flow and sessions.
- Terraform skeleton for ECS Fargate, ECR, ALB, VPC, Security Groups,
  Secrets Manager, CloudWatch, IAM, and optional RDS PostgreSQL.
- Deployment and validation documentation.

## Pending For Real Production-Ready Status

- Publish real images to ECR.
- Create real ACM certificates and DNS.
- Validate `auth-storage-postgres` against real PostgreSQL/RDS.
- Load real secrets in Secrets Manager outside the repository.
- Add TLS/mTLS or equivalent protection for internal traffic.
- Define backup, restore, retention, and rotation.
- Add complete operational observability: alarms, dashboards, and traces.
- Perform threat review and load testing.

## Remaining Risks

- The replay cache is still local per process.
- SQLite mode does not scale horizontally.
- The Gateway already has HTTP health, but still needs validation behind a real ALB.
- The internal TCP/JSON protocol has no mTLS.
- `AUTH_MODE=strict` can resolve Secrets Manager, but real secrets still need to be loaded.
- Business authorization remains outside the demo system.
