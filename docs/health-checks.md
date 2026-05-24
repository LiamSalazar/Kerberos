# Health Checks

Fase 20 agrega health checks HTTP ligeros para los procesos Java y mantiene
checks HTTP simples para los frontends.

## Endpoints Java

Cada proceso Java puede exponer:

- `/health`
- `/metrics`

Respuesta de `/health`:

```json
{"status":"UP","service":"auth-websocket-gateway","version":"0.1.0-SNAPSHOT","uptimeSeconds":12,"storageMode":"sqlite","timestamp":"2026-05-24T00:00:00Z"}
```

No se exponen secretos, cadenas de conexion con password, tickets ni payloads
sensibles.

## Puertos Locales/Docker

- Gateway: `AUTH_HEALTH_PORT=2801`
- AS: `AUTH_AS_HEALTH_PORT=2900` en Compose, `AUTH_HEALTH_PORT=2900` dentro del
  contenedor.
- TGS: `AUTH_TGS_HEALTH_PORT=2901`
- Service: `AUTH_SERVICE_HEALTH_PORT=2902`

`HealthProbe` permite healthchecks Docker sin depender de `curl` o `wget`:

```bash
java -cp /app/classes:/app/dependency/* com.portfolio.auth.core.health.HealthProbe http://127.0.0.1:2801/health
```

## AWS

El ALB debe revisar el Gateway por HTTP en `/health`, puerto `2801`. El trafico
de usuarios usa WSS hacia el puerto `2800`. AS, TGS, Service y RDS permanecen
privados; sus health checks son internos de ECS o red privada.
