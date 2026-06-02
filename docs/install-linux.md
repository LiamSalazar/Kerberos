# Linux Installation Guide

Guide to run the project locally on Linux without Docker and with Docker
Compose. Use demo secrets or placeholders; do not use real secrets in versioned
files.

## 1. Install Tools

Ubuntu/Debian:

```bash
sudo apt update
sudo apt install -y git openjdk-21-jdk maven ca-certificates curl gnupg
```

Docker:

```bash
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"
```

Log out and back in so the `docker` group applies.

## 2. Clone

```bash
git clone <repo-url> Kerberos
cd Kerberos
```

## 3. Validate Maven

```bash
mvn validate
mvn test
```

## 4. Run Without Docker

Open separate terminals:

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

## 5. Prepare Docker Environment

Local SQLite:

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

Validate that the Gateway responds with `status=UP` and that the frontends open
on `localhost:5173` and `localhost:5174`.
