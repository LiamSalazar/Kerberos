# Production Readiness Checklist

Checklist for a later phase. It does not mark the system as production-ready.

## Code And Build

- [x] Maven runs from the repository root.
- [x] CI points to the root.
- [x] Docker Compose points to the root.
- [x] `auth-storage-postgres` exists in the Maven reactor.
- [x] Docker validated on Linux.

## Security

- [x] `AUTH_MODE=strict` exists to reject demo secrets.
- [x] `VERIFY_SESSION` exists before granting access.
- [x] `SecretsProvider` supports env and AWS Secrets Manager without calling AWS in normal tests.
- [x] Terraform references Secrets Manager for ECS tasks.

## Persistence

- [x] Local SQLite with migrations.
- [x] Local persistent audit.
- [x] PostgreSQL implemented as a real module.
- [x] Versioned PostgreSQL migrations.
- [x] Opaque sessions support a shared PostgreSQL repository.

## Operation

- [x] CloudWatch Logs planned in Terraform.
- [x] Real health endpoints for Java processes and the Gateway ALB target.
- [x] Basic in-memory metrics and `/metrics`.

## AWS

- [x] Terraform skeleton created.
- [x] Blueprint updated for ALB/WSS, Gateway `/health`, Secrets Manager, and RDS.
- [x] `terraform fmt` and `terraform validate` executed in an environment with Terraform.
- [x] `terraform plan` reviewed with real non-secret variables.
- [ ] DNS and ACM configured.
- [ ] ECR push validated.
- [ ] ECS deployment validated in a sandbox account.
