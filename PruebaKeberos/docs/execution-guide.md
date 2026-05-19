# Execution Guide

Guia para compilar, probar, ejecutar y auditar localmente sin Docker.

## Requisitos

- Java 17 o superior.
- Maven 3.9+.
- Git.
- Docker no es requisito.

## Maven

Desde `PruebaKeberos`:

```bash
mvn -q -DskipTests compile
mvn test
```

En Fase 8.1 ambos comandos pasaron con Maven disponible por ruta absoluta del
entorno local.

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

Los scripts ejecutan `mvn -q -DskipTests compile` antes de lanzar la clase Java.

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

## Auditoria Modular

Con AS, TGS y Service modulares levantados:

```cmd
scripts\run-audit.bat --iterations 5
```

El runner genera:

- `docs/audits/latest-run.md`
- `docs/audits/latest-run.json`

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
- `AUTH_LEGACY_CLIENT_SECRET`
- `AUTH_LEGACY_CLIENT_TGS_KEY`
- `AUTH_LEGACY_TGS_SECRET`
- `AUTH_LEGACY_CLIENT_SERVICE_KEY`
- `AUTH_LEGACY_SERVICE_SECRET`
- `AUTH_LEGACY_PBKDF2_SALT`

`AUTH_MODE=demo` o `AUTH_MODE=local` permite defaults de demo local.
`AUTH_MODE=strict` exige secretos explicitos y rechaza defaults.

## Estado Legacy

La ruta historica fue retirada del proyecto principal. No hay comandos actuales
para ejecutarla. Ver `docs/legacy-summary.md` para contexto historico.

## Futuro

Docker, Docker Compose, WebSockets y frontend quedan fuera de esta fase y solo
deben introducirse cuando se autorice una fase especifica.
