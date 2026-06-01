# Security Validation Lab

`auth-web-demo` incluye una seccion visual llamada Security Validation Lab. Su
objetivo es mostrar escenarios validados y escenarios de demo sin inventar
pruebas.

## Escenarios

| Escenario | Resultado esperado | Tipo |
| --- | --- | --- |
| Valid client and service | `ACCESS GRANTED`, luego `SESSION_VALID` | live/manual flow |
| Unknown client | `ACCESS DENIED`, `CLIENT_NOT_FOUND` o evidencia `UNKNOWN_CLIENT` | demo security scenario |
| Unknown service | `ACCESS DENIED`, `SERVICE_NOT_FOUND` o `TGS_UNKNOWN_SERVICE` | demo security scenario |
| Invalid session verification | `SESSION_INVALID` | demo security scenario |
| Replay protection | replay rechazado por replay cache | validated by automated tests |
| Expired session | `SESSION_INVALID` con `EXPIRED` | validated by automated tests |
| Sensitive data exposure | tickets/keys/ciphertexts ocultos y `sessionId` enmascarado | validated by UI contract |

## Botones Live

Si el Gateway esta conectado, la UI puede disparar:

- Test Unknown Client.
- Test Unknown Service.
- Test Invalid Session.

Estos botones usan el mismo contrato WebSocket existente. No agregan endpoints
ni cambian el backend.

## Evidencia Automatizada

- Replay: `InMemoryReplayCacheTest` y pruebas de flujo modular.
- Expired session: `GatewaySessionServiceTest` y repositorios de sesion.
- Unknown client/service: pruebas de Gateway y flujo modular.
- Docker/PostgreSQL/Terraform: evidencia documentada en `validation-results/`
  y `docs/project-validation-results.md`.

La seccion visual no sustituye `mvn test`.
