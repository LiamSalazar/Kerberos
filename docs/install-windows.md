# Windows Installation Guide

Guia para ejecutar el proyecto en Windows con PowerShell. CMD y Git Bash pueden
usarse, pero los ejemplos oficiales de esta guia usan PowerShell.

## 1. Instalar Herramientas

Instalar:

- Git for Windows.
- JDK 21 o compatible.
- Maven, o Maven integrado desde el IDE si se usa una terminal configurada.
- Docker Desktop con WSL2 habilitado.

Verificar en PowerShell:

```powershell
git --version
java -version
mvn -version
docker version
docker compose version
```

PowerShell usa `.\script.bat` o `scripts\script.bat`. CMD usa sintaxis similar.
Git Bash usa rutas tipo `scripts/run-as.sh`.

## 2. Clonar

```powershell
git clone <repo-url> Kerberos
cd Kerberos
```

## 3. Validar Maven

```powershell
mvn validate
mvn test
```

## 4. Ejecutar Sin Docker

Abra terminales separadas:

```powershell
scripts\run-as.bat
scripts\run-tgs.bat
scripts\run-service.bat
scripts\run-websocket-gateway.bat
scripts\run-web-demo.bat
scripts\run-sample-login-app.bat
```

Abrir:

```text
http://localhost:5173
http://localhost:5174
```

## 5. Ejecutar Con Docker Compose

SQLite:

```powershell
Copy-Item .env.example .env
docker compose build
docker compose up -d
Invoke-RestMethod http://localhost:2801/health
```

PostgreSQL local:

```powershell
docker compose --env-file .env.postgres --profile postgres-local build
docker compose --env-file .env.postgres --profile postgres-local up -d
docker compose --env-file .env.postgres --profile postgres-local ps
Invoke-RestMethod http://localhost:2801/health
```

Apagar:

```powershell
docker compose --env-file .env.postgres --profile postgres-local down
```

No cambie los puertos `2800`, `2801`, `5173` ni `5174`.
