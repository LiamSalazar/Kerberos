# Execution Guide

Guia para compilar, probar, ejecutar y auditar localmente sin Docker.

## Requisitos

- Java 17 o superior.
- Maven 3.9+.
- Node.js 18+ y npm para `auth-web-demo`.
- Python 3 para servir `sample-login-app` con los scripts locales.
- Git.
- Docker no es requisito.

## Maven

Desde `PruebaKeberos`:

```bash
mvn -q -DskipTests compile
mvn test
```

Comandos principales de verificacion:

```bash
mvn -q -DskipTests compile
mvn test
mvn -pl auth-storage-sqlite -am test
mvn -pl auth-websocket-gateway -am test
```

Fase 15 mantiene esas verificaciones como cierre minimo cuando se toca runtime,
SQLite o Gateway.

## Dependency Audit

```bash
mvn dependency:tree
```

Resumen versionado:

- `docs/audits/maven-dependency-audit.md`

## Ejecutar Runtime Modular Con Scripts

En Windows, abre tres terminales para servidores y una para cliente:

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

Los scripts compilan con Maven y preparan el classpath runtime antes de lanzar
la clase Java. Esto permite usar tanto `AUTH_STORAGE_MODE=memory` como
`AUTH_STORAGE_MODE=sqlite`.

En Linux/macOS:

```bash
scripts/run-as.sh
scripts/run-tgs.sh
scripts/run-service.sh
scripts/run-client.sh
```

## Ejecutar Runtime Modular Manualmente

Compila con Maven:

```bash
mvn -q -DskipTests compile
```

En Windows:

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

Para ejecucion manual con SQLite, usa preferentemente los scripts porque
incluyen `sqlite-jdbc` en el classpath.

## Auditoria Modular

Con AS, TGS y Service modulares levantados:

```cmd
scripts\run-audit.bat --iterations 5
```

El runner genera:

- `docs/audits/latest-run.md`
- `docs/audits/latest-run.json`

## Auditoria De Concurrencia

Con AS, TGS y Service modulares levantados:

```cmd
scripts\run-concurrency-audit.bat --clients 25 --flows 100
```

Linux/macOS:

```bash
scripts/run-concurrency-audit.sh --clients 25 --flows 100
```

El runner genera:

- `docs/audits/concurrency-latest-run.md`
- `docs/audits/concurrency-latest-run.json`

Tambien existe una prueba Maven que levanta servidores en puertos dinamicos:

```bash
mvn -pl auth-client-sdk -am test
```

## SQLite Local

Crear o migrar base demo:

```cmd
scripts\init-sqlite-demo.bat --db data\auth-demo.sqlite
```

Linux/macOS:

```bash
scripts/init-sqlite-demo.sh --db data/auth-demo.sqlite
```

Las migraciones viven en `scripts/sqlite/migrations/` y se registran en
`schema_version`.

Ejecutar servidores en modo SQLite:

```cmd
set AUTH_STORAGE_MODE=sqlite
set AUTH_SQLITE_PATH=data\auth-demo.sqlite
scripts\run-as.bat
```

Repite las mismas variables para:

```cmd
scripts\run-tgs.bat
scripts\run-service.bat
```

Modo memoria por defecto:

```text
AUTH_STORAGE_MODE=memory
```

Administracion local:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients list
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services list
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit list --limit 20
```

Registrar cliente/servicio:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite clients add --id app-client --display-name "App Client" --secret "<secret>"
scripts\sqlite-admin.bat --db data\auth-demo.sqlite services add --id melodyfinder --display-name "MelodyFinder" --secret "<secret>" --endpoint local://melodyfinder
```

## WebSocket Gateway

El gateway no levanta AS, TGS ni Service por su cuenta. Primero ejecuta los tres
servidores modulares y luego:

```cmd
scripts\run-websocket-gateway.bat
```

En Linux/macOS:

```bash
scripts/run-websocket-gateway.sh
```

Por defecto escucha en `127.0.0.1:2800`. Puedes ajustar:

- `AUTH_WS_HOST`
- `AUTH_WS_PORT`

Prueba manual con una herramienta externa compatible con WebSocket, por ejemplo
`websocat`:

```bash
websocat ws://127.0.0.1:2800
```

Mensaje de entrada:

```json
{"type":"START_AUTH_FLOW","requestId":"manual-1","clientId":"1","serviceId":"1"}
```

Tambien responde:

```json
{"type":"PING","requestId":"ping-1"}
```

## Frontend Demo Local

La demo web vive en `auth-web-demo/` y usa HTML, CSS y JavaScript vanilla. No
usa React, Vite, TypeScript, bundler ni dependencias npm externas.

Instalar/validar:

```bash
cd auth-web-demo
npm install
npm run build
```

Ejecutar:

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

Orden recomendado:

```text
Terminal 1: scripts\run-as.bat
Terminal 2: scripts\run-tgs.bat
Terminal 3: scripts\run-service.bat
Terminal 4: scripts\run-websocket-gateway.bat
Terminal 5: scripts\run-web-demo.bat
```

Checklist manual:

- backend modular levantado;
- gateway conectado en `ws://127.0.0.1:2800`;
- frontend conectado;
- boton `Start Auth Flow`;
- eventos `FLOW_*` visibles;
- `FLOW_RESULT success=true`;
- servicio concedido visible.

## Sample Login App

`sample-login-app` no usa npm. Con AS, TGS, Service y Gateway levantados:

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

Validar flujo exitoso con `clientId=1` y `serviceId=1`. Validar fallo con un
`serviceId` inexistente. Ver `docs/sample-login-app.md`.

## Configuracion

Variables comunes:

- `AUTH_MODE`: `demo`, `local` o `strict`.
- `AUTH_AS_PORT`
- `AUTH_TGS_PORT`
- `AUTH_SERVICE_PORT`
- `AUTH_DEMO_CLIENT_ID`
- `AUTH_DEMO_TGS_ID`
- `AUTH_DEMO_SERVICE_ID`
- `AUTH_TICKET_TTL_MINUTES`
- `AUTH_ALLOWED_SKEW_SECONDS`
- `AUTH_REPLAY_WINDOW_SECONDS`
- `AUTH_STORAGE_MODE`: `memory` o `sqlite`.
- `AUTH_SQLITE_PATH`: ruta SQLite local.
- `AUTH_DEMO_CLIENT_SECRET`
- `AUTH_DEMO_CLIENT_TGS_KEY`
- `AUTH_DEMO_TGS_SECRET`
- `AUTH_DEMO_CLIENT_SERVICE_KEY`
- `AUTH_DEMO_SERVICE_SECRET`
- `AUTH_DEMO_PBKDF2_SALT`

`AUTH_MODE=demo` o `AUTH_MODE=local` permite defaults de demo local.
`AUTH_MODE=strict` exige secretos explicitos y rechaza defaults.

## Estado Legacy

La ruta historica fue retirada del proyecto principal. No hay comandos actuales
para ejecutarla. Ver `docs/legacy-summary.md` para contexto historico.

## Futuro

Docker y Docker Compose quedan fuera de esta fase y solo deben introducirse
cuando se autorice una fase especifica.
