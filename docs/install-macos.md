# macOS Installation Guide

Guide to run the project on macOS with Homebrew, Maven, and Docker Desktop.

## 1. Install Homebrew

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

## 2. Install Dependencies

```bash
brew install git openjdk maven
brew install --cask docker
```

Open Docker Desktop and wait until Docker has started:

```bash
docker version
docker compose version
```

## 3. Clone

```bash
git clone <repo-url> Kerberos
cd Kerberos
```

## 4. Validate Maven

```bash
mvn validate
mvn test
```

## 5. Run Without Docker

Use separate terminals:

```bash
scripts/run-as.sh
scripts/run-tgs.sh
scripts/run-service.sh
scripts/run-websocket-gateway.sh
scripts/run-web-demo.sh
scripts/run-sample-login-app.sh
```

Open:

```text
http://localhost:5173
http://localhost:5174
```

## 6. Run With Docker Compose

SQLite:

```bash
cp .env.example .env
docker compose build
docker compose up -d
curl http://localhost:2801/health
```

Local PostgreSQL:

```bash
docker compose --env-file .env.postgres --profile postgres-local build
docker compose --env-file .env.postgres --profile postgres-local up -d
docker compose --env-file .env.postgres --profile postgres-local ps
curl http://localhost:2801/health
```

Stop:

```bash
docker compose --env-file .env.postgres --profile postgres-local down
```
