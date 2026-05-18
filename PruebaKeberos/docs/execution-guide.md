# Execution Guide

Guia para compilar, probar y ejecutar localmente sin Docker.

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

## Ejecutar Runtime Modular

Compila con Maven cuando este disponible:

```bash
mvn -q -DskipTests compile
```

En Windows, abre cuatro terminales:

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

## Verificacion Con javac

Si Maven no esta disponible, puedes compilar la ruta modular asi en PowerShell:

```powershell
New-Item -ItemType Directory -Force build\check\phase47-main | Out-Null
$roots = 'auth-core\src\main\java','auth-crypto\src\main\java','auth-transport\src\main\java','auth-as\src\main\java','auth-tgs\src\main\java','auth-service\src\main\java','auth-client-sdk\src\main\java'
$sources = Get-ChildItem $roots -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -d build\check\phase47-main $sources
```

Smoke modular usado en esta fase:

```powershell
$env:AUTH_AS_PORT='2400'
$env:AUTH_TGS_PORT='2401'
$env:AUTH_SERVICE_PORT='2402'
java -cp build\check\phase47-main com.portfolio.auth.as.AuthenticationServerApp
java -cp build\check\phase47-main com.portfolio.auth.tgs.TicketGrantingServerApp
java -cp build\check\phase47-main com.portfolio.auth.service.ProtectedServiceApp
java -cp build\check\phase47-main com.portfolio.auth.client.ClientCli
```

Cada servidor va en una terminal separada.

## Ejecutar Demo Legacy

La demo legacy sigue viva en `Kerberos/` y `Seguridad/`.

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
- `AUTH_LEGACY_TGS_SECRET`
- `AUTH_LEGACY_SERVICE_SECRET`

Los valores por defecto son solo para demo local.

## Pruebas Esperadas

`mvn test` debe cubrir:

- DTOs y mappers legacy;
- replay cache;
- configuracion;
- AES-GCM;
- JSON codec;
- transporte seguro JSON + AES-GCM;
- integracion modular con AS, TGS, Service y Client;
- casos negativos de replay, servicio inexistente, autenticador invalido y
  requestId repetido.

## Futuro

Docker, Docker Compose, WebSockets y frontend quedan fuera de esta fase y solo
deben introducirse cuando se autorice una fase especifica.
