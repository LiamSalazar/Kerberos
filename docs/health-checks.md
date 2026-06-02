# Health Checks

Phase 20 adds lightweight HTTP health checks for Java processes and keeps
simple HTTP checks for the frontends.

## Java Endpoints

Each Java process can expose:

- `/health`
- `/metrics`

`/health` response:

```json
{"status":"UP","service":"auth-websocket-gateway","version":"0.1.0-SNAPSHOT","uptimeSeconds":12,"storageMode":"sqlite","timestamp":"2026-05-24T00:00:00Z"}
```

Secrets, connection strings with passwords, tickets, and sensitive payloads are
not exposed.

## Local/Docker Ports

- Gateway: `AUTH_HEALTH_PORT=2801`
- AS: `AUTH_AS_HEALTH_PORT=2900` in Compose, `AUTH_HEALTH_PORT=2900` inside the
  container.
- TGS: `AUTH_TGS_HEALTH_PORT=2901`
- Service: `AUTH_SERVICE_HEALTH_PORT=2902`

`HealthProbe` enables Docker healthchecks without depending on `curl` or
`wget`:

```bash
java -cp /app/classes:/app/dependency/* com.portfolio.auth.core.health.HealthProbe http://127.0.0.1:2801/health
```

## AWS

The ALB must check the Gateway over HTTP on `/health`, port `2801`. User traffic
uses WSS toward port `2800`. AS, TGS, Service, and RDS remain private; their
health checks are internal to ECS or the private network.
