# macOS Installation Guide

Guia para ejecutar el proyecto en macOS con Homebrew, Maven y Docker Desktop.

## 1. Instalar Homebrew

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

## 2. Instalar Dependencias

```bash
brew install git openjdk maven
brew install --cask docker
```

Abra Docker Desktop y espere a que Docker este iniciado:

```bash
docker version
docker compose version
```

## 3. Clonar

```bash
git clone <repo-url> Kerberos
cd Kerberos
```

## 4. Validar Maven

```bash
mvn validate
mvn test
```

## 5. Ejecutar Sin Docker

Use terminales separadas:

```bash
scripts/run-as.sh
scripts/run-tgs.sh
scripts/run-service.sh
scripts/run-websocket-gateway.sh
scripts/run-web-demo.sh
scripts/run-sample-login-app.sh
```

Abrir:

```text
http://localhost:5173
http://localhost:5174
```

## 6. Ejecutar Con Docker Compose

SQLite:

```bash
cp .env.example .env
docker compose build
docker compose up -d
curl http://localhost:2801/health
```

PostgreSQL local:

```bash
docker compose --env-file .env.postgres --profile postgres-local build
docker compose --env-file .env.postgres --profile postgres-local up -d
docker compose --env-file .env.postgres --profile postgres-local ps
curl http://localhost:2801/health
```

Apagar:

```bash
docker compose --env-file .env.postgres --profile postgres-local down
```
