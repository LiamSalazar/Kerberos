# Project Validation Results

Resumen de evidencia documentada para el cierre de Fase 23A. Las pruebas
visuales no sustituyen la suite automatizada.

| Validacion | Resultado | Evidencia esperada | Estado |
| --- | --- | --- | --- |
| Maven validate | `mvn validate` | build Maven desde raiz | PASS |
| Maven test | `mvn test` | suite completa | PASS |
| auth-websocket-gateway tests | `mvn -pl auth-websocket-gateway -am test` | contrato WS, sesiones y E2E | PASS |
| auth-storage-sqlite tests | `mvn -pl auth-storage-sqlite -am test` | repositorios, migraciones, auditoria | PASS |
| auth-storage-postgres tests | `mvn -pl auth-storage-postgres -am test` | migraciones y repositorios PostgreSQL | PASS |
| Docker Compose SQLite | `docker compose up -d` | servicios healthy | PASS |
| Docker Compose PostgreSQL local | `docker compose --env-file .env.postgres --profile postgres-local up -d` | servicios healthy con Postgres | PASS |
| Gateway `/health` | `curl http://localhost:2801/health` | `status=UP` | PASS |
| auth-web-demo | `http://localhost:5173` | frontend healthy | PASS |
| sample-login-app | `http://localhost:5174` | frontend healthy | PASS |
| PostgreSQL `auth_sessions` | migraciones Postgres | tabla preparada para sesiones | PASS |
| PostgreSQL `auth_audit_events` | migraciones Postgres | tabla preparada para auditoria | PASS |
| Terraform init | `terraform init` | inicializacion | PASS |
| Terraform validate | `terraform validate` | configuracion valida | PASS |
| Terraform plan HTTP | `terraform plan` | plan HTTP temporal | PASS |
| Terraform plan WSS/HTTPS | plan con ACM placeholder | listener 443 y redirect | PASS |
| Terraform apply | no ejecutar | no se crearon recursos AWS | NOT RUN |

## Aclaraciones

- AWS no se desplego por control de costos.
- El proyecto puede correr localmente para apps propias.
- AWS queda como blueprint cloud-ready para despliegue futuro controlado.
- El Security Validation Lab visualiza resultados y escenarios; no sustituye
  Maven, Docker ni Terraform plan.
- No se deben inventar pruebas no ejecutadas. Si una evidencia se actualiza,
  versionar salida en `validation-results/` o `docs/audits/`.

## Fase 23A Local Rerun

En la corrida local de Fase 23A sobre Windows/PowerShell:

| Comando | Resultado real |
| --- | --- |
| `mvn validate` | PASS |
| `mvn test` | PASS; `PostgresRealIntegrationTest` skipped por no tener endpoint externo de integracion real |
| `mvn -pl auth-websocket-gateway -am test` | PASS |
| `mvn -pl auth-storage-sqlite -am test` | PASS |
| `mvn -pl auth-storage-postgres -am test` | PASS; integracion real skipped |
| `auth-web-demo` build con Node empaquetado | PASS |
| `sample-login-app` JS syntax check con Node empaquetado | PASS |
| Docker Compose | No ejecutable en esta maquina: `docker` no esta en PATH |
| Terraform fmt/validate/plan | No ejecutable en esta maquina: `terraform` no esta en PATH |
| Browser auth-web-demo | PASS en stack local sin Docker: `START_AUTH_FLOW -> FLOW_RESULT -> VERIFY_SESSION -> SESSION_VALID` |
| Browser MelodyFinder | PASS en stack local sin Docker: dashboard abre despues de `SESSION_VALID` |

Las evidencias Docker/Terraform previas siguen versionadas en
`validation-results/`; esta tabla distingue la corrida local actual de esas
evidencias existentes.
