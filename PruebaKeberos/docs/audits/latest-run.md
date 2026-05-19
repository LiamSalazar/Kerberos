# Modular Auth Audit

- Fecha/hora: `2026-05-19T07:34:38.157359Z`
- Java: `19`
- Sistema operativo: `Windows 11 10.0`
- Commit: `48401e5`
- Comando: `com.portfolio.auth.client.audit.ModularAuthAuditRunner --iterations 3`
- Puertos: AS `2500`, TGS `2501`, Service `2502`
- Iteraciones: `3`
- Exitos: `3`
- Fallos: `0`
- Throughput aproximado: `2.232 flujos/s`

| Etapa | Min ms | Max ms | Promedio ms |
| --- | ---: | ---: | ---: |
| AS exchange | 50.900 | 302.761 | 142.694 |
| TGS exchange | 94.906 | 276.531 | 156.855 |
| Service exchange | 79.500 | 241.742 | 139.462 |
| Total | 225.307 | 821.788 | 439.262 |

| Iteracion | Estado | AS ms | TGS ms | Service ms | Total ms | Error |
| ---: | --- | ---: | ---: | ---: | ---: | --- |
| 1 | OK | 302.761 | 276.531 | 241.742 | 821.788 |  |
| 2 | OK | 74.420 | 99.126 | 97.144 | 270.692 |  |
| 3 | OK | 50.900 | 94.906 | 79.500 | 225.307 |  |
