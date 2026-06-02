# Project Validation Results

Summary of documented evidence for the Phase 23A closure. Visual tests do not
replace the automated suite.

| Validation | Result | Expected evidence | Status |
| --- | --- | --- | --- |
| Maven validate | `mvn validate` | Maven build from root | PASS |
| Maven test | `mvn test` | full suite | PASS |
| auth-websocket-gateway tests | `mvn -pl auth-websocket-gateway -am test` | WS contract, sessions, and E2E | PASS |
| auth-storage-sqlite tests | `mvn -pl auth-storage-sqlite -am test` | repositories, migrations, audit | PASS |
| auth-storage-postgres tests | `mvn -pl auth-storage-postgres -am test` | PostgreSQL migrations and repositories | PASS |
| Docker Compose SQLite | `docker compose up -d` | healthy services | PASS |
| Docker Compose PostgreSQL local | `docker compose --env-file .env.postgres --profile postgres-local up -d` | healthy services with Postgres | PASS |
| Gateway `/health` | `curl http://localhost:2801/health` | `status=UP` | PASS |
| auth-web-demo | `http://localhost:5173` | healthy frontend | PASS |
| sample-login-app | `http://localhost:5174` | healthy frontend | PASS |
| PostgreSQL `auth_sessions` | Postgres migrations | table prepared for sessions | PASS |
| PostgreSQL `auth_audit_events` | Postgres migrations | table prepared for audit | PASS |
| Terraform init | `terraform init` | initialization | PASS |
| Terraform validate | `terraform validate` | valid configuration | PASS |
| Terraform plan HTTP | `terraform plan` | temporary HTTP plan | PASS |
| Terraform plan WSS/HTTPS | plan with ACM placeholder | listener 443 and redirect | PASS |
| Terraform apply | do not run | no AWS resources created | NOT RUN |

## Clarifications

- AWS was not deployed for cost control.
- The project can run locally for your own apps.
- AWS remains a cloud-ready blueprint for a future controlled deployment.
- The Security Validation Lab visualizes results and scenarios; it does not
  replace Maven, Docker, or Terraform plan.

## Phase 23A Local Rerun

In the local Phase 23A run on Windows/PowerShell:

| Command | Real result |
| --- | --- |
| `mvn validate` | PASS |
| `mvn test` | PASS |
| `mvn -pl auth-websocket-gateway -am test` | PASS |
| `mvn -pl auth-storage-sqlite -am test` | PASS |
| `mvn -pl auth-storage-postgres -am test` | PASS |
| `auth-web-demo` build with bundled Node | PASS |
| `sample-login-app` JS syntax check with bundled Node | PASS |
| Docker Compose | PASS |
| Terraform fmt/validate/plan | PASS |
| Browser auth-web-demo | PASS |
| Browser MelodyFinder | PASS |
