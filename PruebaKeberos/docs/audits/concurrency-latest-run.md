# Modular Auth Concurrency Audit

- Fecha/hora: `2026-05-22T05:43:26.745823500Z`
- Java: `19`
- Sistema operativo: `Windows 11 10.0`
- Commit: `1160869`
- Comando: `com.portfolio.auth.client.audit.ModularAuthConcurrencyAuditRunner --clients 25 --flows 100`
- Clientes concurrentes: `25`
- Flujos totales: `100`
- Exitos: `100`
- Fallos: `0`
- Throughput aproximado: `9.039 flujos/s`
- P95 total: `5698.616 ms`

| Etapa | Min ms | Max ms | Promedio ms |
| --- | ---: | ---: | ---: |
| AS exchange | 131.544 | 4063.657 | 1255.533 |
| TGS exchange | 359.796 | 1221.686 | 775.459 |
| Service exchange | 194.248 | 1054.867 | 605.769 |
| Total | 947.352 | 5839.143 | 2636.763 |

## Errores Por Tipo

Sin errores.
