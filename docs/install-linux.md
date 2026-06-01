# Linux Installation Guide

Guia para ejecutar el proyecto localmente en Linux sin Docker y con Docker
Compose. Use secretos de demo o placeholders; no use secretos reales en archivos
versionados.

## 1. Instalar Herramientas

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

Cierre sesion y vuelva a entrar para que el grupo `docker` aplique.

## 2. Clonar

```bash
git clone <repo-url> Kerberos
cd Kerberos
```

## 3. Validar Maven

```bash
mvn validate
mvn test
```

## 4. Ejecutar Sin Docker

Abra terminales separadas:

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

## 5. Preparar Entorno Docker

SQLite local:

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

Validar que el Gateway responde `status=UP` y que los frontends abren en
`localhost:5173` y `localhost:5174`.
