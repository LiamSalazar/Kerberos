# Security Validation Lab

`auth-web-demo` includes a visual section called Security Validation Lab. Its
goal is to show validated scenarios and demo scenarios without inventing tests.

## Scenarios

| Scenario | Expected result | Type |
| --- | --- | --- |
| Valid client and service | `ACCESS GRANTED`, then `SESSION_VALID` | live/manual flow |
| Unknown client | `ACCESS DENIED`, `CLIENT_NOT_FOUND` or evidence `UNKNOWN_CLIENT` | demo security scenario |
| Unknown service | `ACCESS DENIED`, `SERVICE_NOT_FOUND` or `TGS_UNKNOWN_SERVICE` | demo security scenario |
| Invalid session verification | `SESSION_INVALID` | demo security scenario |
| Replay protection | replay rejected by replay cache | validated by automated tests |
| Expired session | `SESSION_INVALID` with `EXPIRED` | validated by automated tests |
| Sensitive data exposure | tickets/keys/ciphertexts hidden and `sessionId` masked | validated by UI contract |

## Live Buttons

If the Gateway is connected, the UI can trigger:

- Test Unknown Client.
- Test Unknown Service.
- Test Invalid Session.

These buttons use the same existing WebSocket contract. They do not add
endpoints or change the backend.

## Automated Evidence

- Replay: `InMemoryReplayCacheTest` and modular flow tests.
- Expired session: `GatewaySessionServiceTest` and session repositories.
- Unknown client/service: Gateway and modular flow tests.
- Docker/PostgreSQL/Terraform: evidence documented in `validation-results/`
  and `docs/project-validation-results.md`.

The visual section does not replace `mvn test`.
