# Observability

Phase 20 adds basic observability to operate the stack in Docker and prepare
CloudWatch.

## Structured Logs

`auth-core` includes:

- `StructuredLog`
- `JsonLogFormatter`
- `RequestContext`

The Gateway records JSON events with:

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

Secrets, passwords, tickets, ciphertexts, keys, and full `sessionId` must not be
logged. `JsonLogFormatter` redacts known sensitive fields.

## Metrics

`MetricsRegistry` keeps process-local counters and latencies:

- `flow_success_total`
- `flow_failure_total`
- `session_created_total`
- `session_verified_total`
- `session_invalid_total`
- `request_latency_ms_count`
- `request_latency_ms_sum`
- `request_latency_ms_max`
- `active_sessions_count`

The `/metrics` endpoint is exposed on each Java process health HTTP server. In
Phase 20 this is a simple base for local inspection and CloudWatch; it is not a
complete Prometheus, alarms, or tracing stack.

## CloudWatch

Terraform prepares log groups per service under:

```text
/ecs/<prefix>/<service>
```

Before real AWS usage, alarms for errors, latency, restarts, RDS capacity, and
health check failures remain pending.
