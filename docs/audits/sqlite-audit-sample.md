# SQLite Audit Sample

Safe example of events persisted in `auth_audit_events`.

It contains no secrets, keys, full tickets, ciphertexts, or internal payloads.
The values are representative of a flow started by `sample-login-app`.

| requestId | clientId | serviceId | eventType | status | errorType | latencyMs |
| --- | --- | --- | --- | --- | --- | --- |
| sample-login-1 | 1 | 1 | AUTH_FLOW_STARTED | STARTED | - | 0 |
| sample-login-1 | 1 | 1 | AUTH_FLOW_SUCCEEDED | SUCCESS | - | 24 |
| sample-login-2 | 1 | missing-service | AUTH_FLOW_STARTED | STARTED | - | 0 |
| sample-login-2 | 1 | missing-service | AUTH_FLOW_FAILED | FAILURE | TGS_UNKNOWN_SERVICE | 17 |

Local query:

```cmd
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit list --limit 20
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-request --request-id sample-login-1
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-client --client-id 1
scripts\sqlite-admin.bat --db data\auth-demo.sqlite audit by-service --service-id 1
```
