# Concurrency

Phase 14 adds concurrency tests and audit for the modular AS -> TGS -> Service
path. The goal is to verify that multiple simultaneous flows can complete
without `requestId` collisions or false-positive replay detections.

## Automated Test

The Maven suite includes `ModularAuthConcurrencyTest` in `auth-client-sdk`.

The test starts modular AS, TGS, and Service on dynamic ports and runs:

- 10 concurrent clients;
- 50 total flows;
- unique `requestId` values and authenticators per flow;
- validation of successes, failures, average latency, p95, and throughput.

Run:

```bash
mvn -pl auth-client-sdk -am test
```

It is also covered by:

```bash
mvn test
```

## Reproducible Audit

With AS, TGS, and Service running:

```cmd
scripts\run-concurrency-audit.bat --clients 25 --flows 100
```

Linux/macOS:

```bash
scripts/run-concurrency-audit.sh --clients 25 --flows 100
```

The `ModularAuthConcurrencyAuditRunner` generates:

- `docs/audits/concurrency-latest-run.md`
- `docs/audits/concurrency-latest-run.json`

The report records date/time, Java, operating system, commit, command,
concurrent clients, total flows, successes, failures, minimum latency, maximum
latency, average, p95, approximate throughput, and errors by type.

It does not record secrets, keys, full tickets, ciphertexts, or sensitive
cryptographic material.

The persistent SQLite audit from Phase 15 is independent from this concurrency
audit. The Gateway records per-flow events in `auth_audit_events` when running
with `AUTH_STORAGE_MODE=sqlite`; the concurrency runner keeps its evidence in
Markdown/JSON under `docs/audits/`.

## Latest Versioned Evidence

The latest versioned run was:

- concurrent clients: 25;
- total flows: 100;
- successes: 100;
- failures: 0;
- approximate throughput: 9.039 flows/s;
- total p95: 5698.616 ms.

See `docs/audits/concurrency-latest-run.md` for details.

## Limits

- The test uses local servers in memory or the mode configured by the test.
- It does not measure distributed behavior across multiple machines.
- The replay cache remains process-local.
- It does not replace real load testing or production observability.
