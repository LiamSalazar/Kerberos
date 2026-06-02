# Modular Auth Concurrency Audit

- Date/time: `2026-05-22T05:43:26.745823500Z`
- Java: `19`
- Operating system: `Windows 11 10.0`
- Commit: `1160869`
- Command: `com.portfolio.auth.client.audit.ModularAuthConcurrencyAuditRunner --clients 25 --flows 100`
- Concurrent clients: `25`
- Total flows: `100`
- Successes: `100`
- Failures: `0`
- Approximate throughput: `9.039 flows/s`
- Total P95: `5698.616 ms`

| Stage | Min ms | Max ms | Average ms |
| --- | ---: | ---: | ---: |
| AS exchange | 131.544 | 4063.657 | 1255.533 |
| TGS exchange | 359.796 | 1221.686 | 775.459 |
| Service exchange | 194.248 | 1054.867 | 605.769 |
| Total | 947.352 | 5839.143 | 2636.763 |

## Errors By Type

No errors.
