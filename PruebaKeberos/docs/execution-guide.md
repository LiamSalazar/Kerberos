# Execution Guide

Esta guia documenta como compilar, probar y ejecutar el proyecto localmente sin
Docker. La ruta funcional principal sigue siendo la demo legacy en `Kerberos/`
y `Seguridad/`.

Fase documentada: **Fase 3.5: estabilizacion verificable y cierre de migracion
controlada inicial**.

## Requisitos

- Java 17 o superior recomendado.
- `javac` y `java` disponibles en el PATH.
- Maven 3.9+ recomendado para pruebas de los modulos `auth-*`.
- Git para clonar el repositorio.
- Docker no se usa todavia.

Verifica el entorno:

```bash
java -version
javac -version
mvn -version
git --version
```

Si `mvn` no aparece, instala Maven o agrega su carpeta `bin` al PATH. La demo
legacy puede compilarse con `javac` aunque Maven no este disponible.

## Clonar

```bash
git clone <repo-url>
cd PruebaKeberos
```

Si el repositorio fue clonado en otra carpeta, entra a la carpeta que contiene
el `pom.xml` raiz, `Kerberos/`, `Seguridad/` y los modulos `auth-*`.

## Compilar Modulos Maven

```bash
mvn -q -DskipTests compile
```

Este comando compila la arquitectura modular. El codigo legacy vive fuera de los
modulos Maven y se compila con los comandos `javac` de abajo para la demo.

Resultado real en esta ejecucion de fase: el comando fue intentado y fallo
porque `mvn` no estaba en el PATH (`CommandNotFoundException` en PowerShell).

## Ejecutar Pruebas

```bash
mvn test
```

Pruebas esperadas:

- mappers legacy en `auth-transport`;
- transporte Java Object;
- replay cache en `auth-core`;
- configuracion basica en `auth-core`;
- AES-GCM en `auth-crypto`.

Resultado real en esta ejecucion de fase: el comando fue intentado y fallo
porque `mvn` no estaba en el PATH. Las fuentes principales y las pruebas
compilaron con `javac` usando Java/Javac 19 y jars JUnit locales, pero eso es
solo una verificacion complementaria.

Tambien se ejecuto un smoke local AS -> TGS -> Service -> Client con puertos
alternos `2300`, `2301` y `2302`. El cliente completo el flujo y recibio el
mensaje de servicio concedido.

## Compilar Demo Legacy

### Windows CMD

```cmd
if not exist build\classes mkdir build\classes
(for /r Kerberos %f in (*.java) do @echo %f) > sources.txt
(for /r Seguridad %f in (*.java) do @echo %f) >> sources.txt
(for /r auth-core\src\main\java %f in (*.java) do @echo %f) >> sources.txt
(for /r auth-transport\src\main\java %f in (*.java) do @echo %f) >> sources.txt
(for /r auth-crypto\src\main\java %f in (*.java) do @echo %f) >> sources.txt
javac -d build\classes @sources.txt
```

### PowerShell

```powershell
New-Item -ItemType Directory -Force build\classes | Out-Null
$sources = Get-ChildItem Kerberos,Seguridad,auth-core\src\main\java,auth-transport\src\main\java,auth-crypto\src\main\java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -d build\classes $sources
```

Verificacion complementaria usada en Fase 3.5:

```powershell
New-Item -ItemType Directory -Force build\check\phase35 | Out-Null
$sources = Get-ChildItem Kerberos,Seguridad,auth-core\src\main\java,auth-transport\src\main\java,auth-crypto\src\main\java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -d build\check\phase35 $sources
```

### Linux/macOS

```bash
mkdir -p build/classes
find Kerberos Seguridad auth-core/src/main/java auth-transport/src/main/java auth-crypto/src/main/java -name "*.java" > sources.txt
javac -d build/classes @sources.txt
```

## Ejecutar Demo Legacy

Levanta los procesos en terminales separadas y en este orden.

### Windows CMD

Terminal 1:

```cmd
java -cp build\classes Kerberos.AuthenticationServer
```

Terminal 2:

```cmd
java -cp build\classes Kerberos.TicketGrantingServer
```

Terminal 3:

```cmd
java -cp build\classes Kerberos.ServiceServer
```

Terminal 4:

```cmd
java -cp build\classes Kerberos.Client
```

### Linux/macOS

```bash
java -cp build/classes Kerberos.AuthenticationServer
```

```bash
java -cp build/classes Kerberos.TicketGrantingServer
```

```bash
java -cp build/classes Kerberos.ServiceServer
```

```bash
java -cp build/classes Kerberos.Client
```

Ejecuta cada comando en una terminal separada.

## Orden Recomendado

1. `Kerberos.AuthenticationServer`
2. `Kerberos.TicketGrantingServer`
3. `Kerberos.ServiceServer`
4. `Kerberos.Client`

El cliente realiza el flujo AS -> TGS -> Service y debe imprimir el mensaje de
servicio concedido.

## Configuracion

Los defaults estan en `AuthConfig` y son compatibles con la demo local. Puedes
sobrescribirlos por variables de entorno:

- `AUTH_AS_PORT`
- `AUTH_TGS_PORT`
- `AUTH_SERVICE_PORT`
- `AUTH_LEGACY_CLIENT_SECRET`
- `AUTH_LEGACY_CLIENT_TGS_KEY`
- `AUTH_LEGACY_TGS_SECRET`
- `AUTH_LEGACY_CLIENT_SERVICE_KEY`
- `AUTH_LEGACY_SERVICE_SECRET`
- `AUTH_TICKET_TTL_MINUTES`
- `AUTH_ALLOWED_SKEW_SECONDS`
- `AUTH_REPLAY_WINDOW_SECONDS`

Estos valores son para demo local. No son secretos reales de produccion.

## Problemas Comunes

### `mvn` no se reconoce

Maven no esta instalado o no esta en el PATH. Instala Maven y verifica:

```bash
mvn -version
```

Mientras tanto, puedes ejecutar la demo legacy con `javac`.

### `Connection refused`

El cliente se ejecuto antes de levantar AS, TGS o Service. Inicia los servidores
en el orden recomendado.

### Puerto ocupado

Algun proceso ya usa `2000`, `2001` o `2002`. Cierra procesos anteriores o
sobrescribe puertos con variables de entorno.

### Error de clase no encontrada

Recompila incluyendo `Kerberos`, `Seguridad`, `auth-core`, `auth-transport` y
`auth-crypto`. La demo legacy ahora importa clases de la migracion modular.

## Docker

Docker no se usa en esta fase y no es requisito actual. Docker y Docker Compose
quedan como trabajo futuro para despliegue reproducible al final del roadmap.
