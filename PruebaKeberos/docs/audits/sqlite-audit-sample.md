# SQLite Audit Sample

Ejemplo seguro de eventos persistidos en `auth_audit_events`.

No contiene secretos, claves, tickets completos, ciphertexts ni payloads
internos. Los valores son representativos de un flujo iniciado por
`sample-login-app`.

| requestId | clientId | serviceId | eventType | status | errorType | latencyMs |
| --- | --- | --- | --- | --- | --- | --- |
| sample-login-1 | 1 | 1 | AUTH_FLOW_STARTED | STARTED | - | 0 |
| sample-login-1 | 1 | 1 | AUTH_FLOW_SUCCEEDED | SUCCESS | - | 24 |
| sample-login-2 | 1 | missing-service | AUTH_FLOW_STARTED | STARTED | - | 0 |
| sample-login-2 | 1 | missing-service | AUTH_FLOW_FAILED | FAILURE | TGS_UNKNOWN_SERVICE | 17 |

Consulta local:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit list --limit 20
```
