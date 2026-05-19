# Execution Guide

Guia para compilar, probar, ejecutar y auditar localmente sin Docker.

## Requisitos

- Java 17 o superior.
- Maven 3.9+ para validacion principal.
- Git.
- Docker no es requisito.

Verifica:

```bash
java -version
javac -version
mvn -version
git --version
```

## Maven

Desde la raiz:

```bash
mvn -q -DskipTests compile
mvn test
```

En esta sesion `mvn` no estaba disponible en PATH y ambos comandos fallaron con
`CommandNotFoundException`. Se compilo con `javac` como verificacion
complementaria, pero eso no reemplaza `mvn test`.

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
Si Maven no esta instalado, fallaran con el error real de entorno.

## Ejecutar Runtime Modular Manualmente

Compila con Maven cuando este disponible:

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

Orden:

1. `AuthenticationServerApp`
2. `TicketGrantingServerApp`
3. `ProtectedServiceApp`
4. `ClientCli`

El cliente debe imprimir el mensaje del recurso protegido modular.

## Auditoria Modular

Con AS, TGS y Service modulares levantados:

```cmd
scripts\run-audit.bat --iterations 5
```

Variables utiles:

- `AUTH_AUDIT_ITERATIONS`: iteraciones por defecto.
- `AUTH_AUDIT_OUTPUT_DIR`: carpeta de salida; por defecto `docs/audits`.
- `AUTH_AS_PORT`, `AUTH_TGS_PORT`, `AUTH_SERVICE_PORT`: puertos a medir.

El runner genera:

- `docs/audits/latest-run.md`
- `docs/audits/latest-run.json`

La corrida registrada en esta fase uso puertos `2500`, `2501`, `2502` y produjo
3 exitos, 0 fallos.

## Verificacion Con javac

Si Maven no esta disponible, puedes compilar la ruta modular asi en PowerShell:

```powershell
New-Item -ItemType Directory -Force build\check\phase8-main | Out-Null
$roots = 'auth-core\src\main\java','auth-crypto\src\main\java','auth-transport\src\main\java','auth-as\src\main\java','auth-tgs\src\main\java','auth-service\src\main\java','auth-client-sdk\src\main\java'
$sources = Get-ChildItem $roots -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d build\check\phase8-main $sources
```

Esto es solo una verificacion auxiliar. La aceptacion formal sigue siendo Maven.

## Ejecutar Demo Legacy

La demo legacy sigue viva en `Kerberos/` y `Seguridad/`, pero ya no es la ruta
principal del proyecto.

Compilar con PowerShell:

```powershell
New-Item -ItemType Directory -Force build\classes | Out-Null
$sources = Get-ChildItem Kerberos,Seguridad,auth-core\src\main\java,auth-transport\src\main\java,auth-crypto\src\main\java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -d build\classes $sources
```

Terminales separadas:

```cmd
java -cp build\classes Kerberos.AuthenticationServer
```

```cmd
java -cp build\classes Kerberos.TicketGrantingServer
```

```cmd
java -cp build\classes Kerberos.ServiceServer
```

```cmd
java -cp build\classes Kerberos.Client
```

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

## Pruebas Esperadas

`mvn test` debe cubrir:

- DTOs y mappers legacy;
- replay cache;
- configuracion demo/strict;
- AES-GCM;
- JSON codec;
- transporte seguro JSON + AES-GCM;
- integracion modular con AS, TGS, Service y Client;
- replay, servicio inexistente, cliente inexistente, tickets expirados,
  autenticadores invalidos, payloads invalidos, servidor no disponible y
  concurrencia basica.

## Futuro

Docker, Docker Compose, WebSockets y frontend quedan fuera de esta fase y solo
deben introducirse cuando se autorice una fase especifica.
