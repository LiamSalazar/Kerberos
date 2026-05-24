# Observability

Fase 20 agrega observabilidad basica para operar el stack en Docker y preparar
CloudWatch.

## Logs Estructurados

`auth-core` incluye:

- `StructuredLog`
- `JsonLogFormatter`
- `RequestContext`

El Gateway registra eventos JSON con:

- `timestamp`
- `level`
- `service`
- `requestId`
- `clientId`
- `serviceId`
- `event`
- `status`
- `latencyMs`
- `errorType`

No deben registrarse secretos, passwords, tickets, ciphertexts, claves ni
`sessionId` completo. `JsonLogFormatter` redacta campos sensibles conocidos.

## Metricas

`MetricsRegistry` mantiene contadores y latencias en memoria del proceso:

- `flow_success_total`
- `flow_failure_total`
- `session_created_total`
- `session_verified_total`
- `session_invalid_total`
- `request_latency_ms_count`
- `request_latency_ms_sum`
- `request_latency_ms_max`
- `active_sessions_count`

El endpoint `/metrics` se expone en el servidor HTTP de health de cada proceso
Java. En Fase 20 es una base simple para inspeccion local y CloudWatch; no es
un stack completo de Prometheus, alarmas ni tracing.

## CloudWatch

Terraform prepara log groups por servicio bajo:

```text
/ecs/<prefix>/<service>
```

Antes de AWS real quedan pendientes alarmas por errores, latencia, reinicios,
capacidad de RDS y fallos de health check.
