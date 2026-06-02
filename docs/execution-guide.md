# Execution Guide

Guide to compile, test, run, and audit locally. Execution without Docker remains
supported; Docker Compose is optional for a reproducible demo.

## Requirements

- Java 17 or higher.
- Maven 3.9+.
- Node.js 18+ and npm for `auth-web-demo`.
- Python 3 to serve `sample-login-app` with the local scripts.
- Git.
- Docker Desktop is optional for `docker-compose.yml`.

## Maven

From the repository root:

```bash
mvn -q -DskipTests compile
mvn test
```

Main verification commands:

```bash
mvn -q -DskipTests compile
mvn test
mvn -pl auth-storage-sqlite -am test
mvn -pl auth-websocket-gateway -am test
```

Phase 20 keeps these checks as the minimum closure when runtime, SQLite,
PostgreSQL, or Gateway is touched.

## Dependency Audit

```bash
mvn dependency:tree
```

Versioned summary:

- `docs/audits/maven-dependency-audit.md`

## Run Modular Runtime With Scripts

On Windows, open three terminals for servers and one for the client:

```cmd
scripts\run-as.bat
```

```cmd
scripts\run-tgs.bat
```

```cmd
scripts\run-service.bat
```

```cmd
scripts\run-client.bat
```

The scripts compile with Maven and prepare the runtime classpath before
launching the Java class. This allows `AUTH_STORAGE_MODE=memory`,
`AUTH_STORAGE_MODE=sqlite`, or `AUTH_STORAGE_MODE=postgres` when the classpath
includes the corresponding storage module.

On Linux/macOS:

```bash
scripts/run-as.sh
scripts/run-tgs.sh
scripts/run-service.sh
scripts/run-client.sh
```

## Run Modular Runtime Manually

Compile with Maven:

```bash
mvn -q -DskipTests compile
```

On Windows:

```cmd
java -cp auth-as\target\classes;auth-core\target\classes;auth-crypto\target\classes;auth-transport\target\classes com.portfolio.auth.as.AuthenticationServerApp
```

```cmd
java -cp auth-tgs\target\classes;auth-core\target\classes;auth-crypto\target\classes;auth-transport\target\classes com.portfolio.auth.tgs.TicketGrantingServerApp
```

```cmd
java -cp auth-service\target\classes;auth-core\target\classes;auth-crypto\target\classes;auth-transport\target\classes com.portfolio.auth.service.ProtectedServiceApp
```

```cmd
java -cp auth-client-sdk\target\classes;auth-core\target\classes;auth-crypto\target\classes;auth-transport\target\classes com.portfolio.auth.client.ClientCli
```

For manual execution with SQLite, prefer the scripts because they include
`sqlite-jdbc` in the classpath.

## Modular Audit

With modular AS, TGS, and Service running:

```cmd
scripts\run-audit.bat --iterations 5
```

The runner generates:

- `docs/audits/latest-run.md`
- `docs/audits/latest-run.json`

## Concurrency Audit

With modular AS, TGS, and Service running:

```cmd
scripts\run-concurrency-audit.bat --clients 25 --flows 100
```

Linux/macOS:

```bash
scripts/run-concurrency-audit.sh --clients 25 --flows 100
```

The runner generates:

- `docs/audits/concurrency-latest-run.md`
- `docs/audits/concurrency-latest-run.json`

There is also a Maven test that starts servers on dynamic ports:

```bash
mvn -pl auth-client-sdk -am test
```

## Local SQLite

Create or migrate the demo database:

```cmd
scripts\init-sqlite-demo.bat --db data\auth-demo.sqlite
```

Linux/macOS:

```bash
scripts/init-sqlite-demo.sh --db data/auth-demo.sqlite
```

Migrations live in `scripts/sqlite/migrations/` and are recorded in
`schema_version`.

Run servers in SQLite mode:

```cmd
set AUTH_STORAGE_MODE=sqlite
set AUTH_SQLITE_PATH=data\auth-demo.sqlite
scripts\run-as.bat
```

Repeat the same variables for:

```cmd
scripts\run-tgs.bat
scripts\run-service.bat
```

Default memory mode:

```text
AUTH_STORAGE_MODE=memory
```

Local administration:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients list
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services list
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit list --limit 20
```

Register client/service:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients add --id app-client --display-name "App Client" --secret "<secret>"
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services add --id melodyfinder --display-name "MelodyFinder" --secret "<secret>" --endpoint local://melodyfinder
```

## WebSocket Gateway

The Gateway does not start AS, TGS, or Service by itself. First run the three
modular servers, then:

```cmd
scripts\run-websocket-gateway.bat
```

On Linux/macOS:

```bash
scripts/run-websocket-gateway.sh
```

By default it listens on `127.0.0.1:2800`. You can adjust:

- `AUTH_WS_HOST`
- `AUTH_WS_PORT`

Manual test with an external WebSocket-compatible tool, for example `websocat`:

```bash
websocat ws://127.0.0.1:2800
```

Input message:

```json
{"type":"START_AUTH_FLOW","requestId":"manual-1","clientId":"1","serviceId":"1"}
```

It also responds to:

```json
{"type":"PING","requestId":"ping-1"}
```

## Local Frontend Demo

The web demo lives in `auth-web-demo/` and uses vanilla HTML, CSS, and
JavaScript. It does not use React, Vite, TypeScript, a bundler, or external npm
dependencies.

Install/validate:

```bash
cd auth-web-demo
npm install
npm run build
```

Run:

```cmd
scripts\run-web-demo.bat
```

Linux/macOS:

```bash
scripts/run-web-demo.sh
```

Default:

```text
http://127.0.0.1:5173
```

Recommended order:

```text
Terminal 1: scripts\run-as.bat
Terminal 2: scripts\run-tgs.bat
Terminal 3: scripts\run-service.bat
Terminal 4: scripts\run-websocket-gateway.bat
Terminal 5: scripts\run-web-demo.bat
```

Manual checklist:

- modular backend running;
- gateway connected at `ws://127.0.0.1:2800`;
- frontend connected;
- `Start Auth Flow` button;
- visible `FLOW_*` events;
- `FLOW_RESULT success=true` with `sessionId` and `sessionExpiresAt`;
- `VERIFY_SESSION` responds with `SESSION_VALID`;
- granted service visible.

## Sample Login App

`sample-login-app` does not use npm. With AS, TGS, Service, and Gateway running:

```cmd
scripts\run-sample-login-app.bat
```

Linux/macOS:

```bash
scripts/run-sample-login-app.sh
```

Default:

```text
http://127.0.0.1:5174
```

Validate a successful flow with `clientId=1` and `serviceId=1`. Validate failure
with a nonexistent `serviceId`. The app unlocks the dashboard only after
`SESSION_VALID`, not only because `FLOW_RESULT.success=true`. See
`docs/sample-login-app.md`.

## Configuration

Common variables:

- `AUTH_MODE`: `demo` or `strict`.
- `AUTH_AS_PORT`
- `AUTH_TGS_PORT`
- `AUTH_SERVICE_PORT`
- `AUTH_DEMO_CLIENT_ID`
- `AUTH_DEMO_TGS_ID`
- `AUTH_DEMO_SERVICE_ID`
- `AUTH_TICKET_TTL_MINUTES`
- `AUTH_ALLOWED_SKEW_SECONDS`
- `AUTH_REPLAY_WINDOW_SECONDS`
- `AUTH_STORAGE_MODE`: `memory`, `sqlite`, or `postgres`.
- `AUTH_SQLITE_PATH`: local SQLite path.
- `AUTH_POSTGRES_URL`
- `AUTH_POSTGRES_USER`
- `AUTH_POSTGRES_PASSWORD`
- `AUTH_POSTGRES_SSL_MODE`
- `AUTH_DEMO_CLIENT_SECRET`
- `AUTH_DEMO_TGS_SECRET`
- `AUTH_DEMO_SERVICE_SECRET`
- `AUTH_DEMO_PBKDF2_SALT`
- `AUTH_ALLOWED_ORIGINS`
- `AUTH_SESSION_TTL_SECONDS`
- `AUTH_SESSION_MAX_TTL_SECONDS`
- `AUTH_SESSION_STORAGE_MODE`: `memory`, `sqlite`, or `postgres`.
- `AUTH_REQUIRE_SESSION_VERIFY`
- `AUTH_SECRET_PROVIDER`
- `AUTH_AWS_REGION`
- `AUTH_SECRET_CLIENT_SECRET_ID`
- `AUTH_SECRET_TGS_SECRET_ID`
- `AUTH_SECRET_SERVICE_SECRET_ID`
- `AUTH_SECRET_POSTGRES_PASSWORD_ID`

`AUTH_MODE=demo` allows local demo defaults and shows a warning.
`AUTH_MODE=strict` requires explicit `AUTH_DEMO_CLIENT_SECRET`,
`AUTH_DEMO_TGS_SECRET`, and `AUTH_DEMO_SERVICE_SECRET` and rejects defaults.
`AUTH_DEMO_PBKDF2_SALT` is a local derivation parameter, not an external app
secret.

## Docker Compose

Prepare:

```cmd
copy .env.example .env
scripts\docker-up.bat
```

Linux/macOS:

```bash
cp .env.example .env
scripts/docker-up.sh
```

Open:

```text
http://localhost:5173
http://localhost:5174
ws://localhost:2800
```

View logs:

```cmd
scripts\docker-logs.bat
```

Stop:

```cmd
scripts\docker-down.bat
```

AS/TGS/Service do not publish ports to the host in Compose. The
`auth-sqlite-data` volume keeps `/data/auth-demo.sqlite` until
`docker compose down -v` is executed.

## Legacy Status

The historical path was removed from the main project. There are no current
commands to run it. See `docs/legacy-summary.md` for historical context.

## Future

TLS/mTLS, secrets vault, and cloud deployment are outside this phase. For future
cloud tests, use `wss://`; `ws://` remains limited to local development.
