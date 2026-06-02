# Windows Installation Guide

Guide to run the project on Windows with PowerShell. CMD and Git Bash can also
be used, but the official examples in this guide use PowerShell.

## 1. Install Tools

Install:

- Git for Windows.
- JDK 21 or compatible.
- Maven, or integrated Maven from the IDE if using a configured terminal.
- Docker Desktop with WSL2 enabled.

Verify in PowerShell:

```powershell
git --version
java -version
mvn -version
docker version
docker compose version
```

PowerShell uses `.\script.bat` or `scripts\script.bat`. CMD uses similar
syntax. Git Bash uses paths such as `scripts/run-as.sh`.

## 2. Clone

```powershell
git clone <repo-url> Kerberos
cd Kerberos
```

## 3. Validate Maven

```powershell
mvn validate
mvn test
```

## 4. Run Without Docker

Open separate terminals:

```powershell
scripts\run-as.bat
scripts\run-tgs.bat
scripts\run-service.bat
scripts\run-websocket-gateway.bat
scripts\run-web-demo.bat
scripts\run-sample-login-app.bat
```

Open:

```text
http://localhost:5173
http://localhost:5174
```

## 5. Run With Docker Compose

SQLite:

```powershell
Copy-Item .env.example .env
docker compose build
docker compose up -d
Invoke-RestMethod http://localhost:2801/health
```

Local PostgreSQL:

```powershell
docker compose --env-file .env.postgres --profile postgres-local build
docker compose --env-file .env.postgres --profile postgres-local up -d
docker compose --env-file .env.postgres --profile postgres-local ps
Invoke-RestMethod http://localhost:2801/health
```

Stop:

```powershell
docker compose --env-file .env.postgres --profile postgres-local down
```

Do not change ports `2800`, `2801`, `5173`, or `5174`.
