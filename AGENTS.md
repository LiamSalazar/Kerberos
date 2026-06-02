# AGENTS.md

Permanent instructions for future Codex work in this repository:

- Do not state that the project is official MIT Kerberos.
- Do not state that the system is ready for critical production use.
- The main path is modular and lives in `auth-*`.
- Do not reintroduce retired legacy folders without explicit authorization.
- Keep changes small, reviewable, and explained.
- Run `mvn test` when code or behavior is touched; if it fails, document the
  real result.
- Keep `README.md` and `docs/` synchronized with technical changes.
- Do not print secrets, keys, full decrypted tickets, or sensitive payloads in
  new logs.
- Prefer typed DTOs over `HashMap<String,Object>` in new code.
- The new modular path must remain free of dependencies on historical packages
  and Java serialization as the main contract.
- `auth-websocket-gateway` is a separate integration layer; it must not replace
  or couple WebSockets inside `auth-as`, `auth-tgs`, or `auth-service`.
- `auth-web-demo` is a local vanilla frontend demo; keep it decoupled from the
  backend and communicating only with `auth-websocket-gateway`.
- `sample-login-app` is a vanilla mini app for integrators; keep it decoupled
  from the backend and without frontend frameworks unless a future phase
  explicitly authorizes them.
- External apps must not grant access only because `FLOW_RESULT.success=true`;
  they must use `VERIFY_SESSION` and accept access only with `SESSION_VALID`.
- Keep Gateway opaque sessions without RSA, without JWT, without CA, and without
  exposing secrets, tickets, ciphertexts, or keys to the frontend.
- For modular runtime changes, cover at least JSON codec, AES-GCM, and the
  AS -> TGS -> Service flow when viable.
- Keep `docs/audits/legacy-dependency-audit.md` updated when touching legacy
  independence.
- If a modular audit is run, document or version the evidence in
  `docs/audits/latest-run.md` and `docs/audits/latest-run.json`.
- If a concurrency audit is run, document or version the evidence in
  `docs/audits/concurrency-latest-run.md` and
  `docs/audits/concurrency-latest-run.json`.
- Keep `docs/audits/maven-dependency-audit.md` updated when Maven
  dependencies/POMs are touched.
- Keep `docs/audits/sqlite-audit-sample.md` and
  `docs/audits/sqlite-audit-sample.json` synchronized when the SQLite audit
  format changes.
- Respect `AUTH_MODE=demo` for demo and `AUTH_MODE=strict` for validation
  without default secrets.
- Keep `AUTH_STORAGE_MODE=memory` as the default demo mode and
  `AUTH_STORAGE_MODE=sqlite` as a verifiable local integration.
- Keep `AUTH_STORAGE_MODE=postgres` as the prepared cloud/RDS mode, without
  removing SQLite or memory mode.
- In cloud, prefer `AUTH_SECRET_PROVIDER=aws-secrets-manager` and do not version
  real secrets or tfvars with passwords.
- For multiple Gateway instances, use opaque sessions persisted in a shared
  database; do not depend on local memory.
- Keep health checks, structured logs, and metrics free of secrets, tickets,
  ciphertexts, keys, and full `sessionId`.
- Do not version generated databases `*.db`, `*.sqlite`, or `*.sqlite3`.
- Prefer honest documentation over exaggerated claims.
- Keep local execution without Docker as a current requirement.
- Docker and Docker Compose are authorized only as reproducible local
  deployment; do not present them as production.
- Do not version local portable tools such as `tools/`.
- Do not introduce Spring Boot. Do not add frontend frameworks unless a future
  phase explicitly authorizes them.
- Do not add npm or frameworks to `sample-login-app` without explicit
  authorization.
- Always explain what changed, how to test it, and what remains pending.
