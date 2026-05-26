# SQLite Docker Validation

Status: PASS

Validated:
- All Docker Compose services are healthy.
- Gateway health endpoint responds UP.
- storageMode reports sqlite.
- Web demo is reachable on localhost:5173.
- Sample login app is reachable on localhost:5174.
- Negative auth attempts are logged as controlled structured errors.
- No real secrets, tickets, ciphertexts, or passwords were observed in logs.

Observed expected warnings:
- AUTH_MODE=demo allows default demo secrets and must not be used for critical production.

Observed controlled errors:
- TGS_UNKNOWN_SERVICE for invalid serviceId.
- UNKNOWN_CLIENT for invalid clientId.

Conclusion:
SQLite mode is operationally validated for local Docker.
