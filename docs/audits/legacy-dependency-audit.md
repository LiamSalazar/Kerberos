# Legacy Dependency Audit

Execution date: 2026-05-19

Objective: confirm whether the `auth-*` modules, including
`auth-websocket-gateway`, depend on historical packages, Java serialization, or
legacy conceptual adapters after Phase 9 + Phase 10.

## Executed Commands

```powershell
& 'C:\Program Files\NetBeans-19\netbeans\java\maven\bin\mvn.cmd' -q -DskipTests compile
& 'C:\Program Files\NetBeans-19\netbeans\java\maven\bin\mvn.cmd' test
```

Dependency search:

```powershell
rg -n "import Kerberos|import Seguridad|Kerberos\.|Seguridad\." auth-as\src auth-tgs\src auth-service\src auth-client-sdk\src auth-core\src auth-crypto\src auth-transport\src auth-websocket-gateway\src
```

```powershell
rg -n "\bKerberos[.]|\bSeguridad[.]|AESUtils|SealedObject|ObjectInputStream|ObjectOutputStream" auth-as\src auth-tgs\src auth-service\src auth-client-sdk\src auth-core\src auth-crypto\src auth-transport\src auth-websocket-gateway\src
```

Additional Phase 9 closure search:

```powershell
rg -n "legacy[A-Z]|ObjectInputStream|ObjectOutputStream|SealedObject|AESUtils|com\.portfolio\.auth\.transport\.legacy|Legacy.*Mapper|transport\.legacy" auth-as auth-tgs auth-service auth-client-sdk auth-core auth-crypto auth-transport auth-websocket-gateway README.md requirements.txt docs AGENTS.md
```

## Maven Result

- Compile: passed.
- Test: passed.
- Current `mvn test` result: 60 tests, 0 failures, 0 errors, 0 skipped.
- `auth-websocket-gateway` is part of the Maven reactor and its 20 tests pass,
  including real E2E with a WebSocket client.

## References Found

- No imports or direct references from `auth-*` to historical packages were
  found.
- No `ObjectInputStream`, `ObjectOutputStream`, `SealedObject`, or `AESUtils`
  remain in the modular path.
- `auth-transport/javaio` and `auth-transport/legacy` do not exist in the
  current source tree.
- No historical configuration aliases remain in `AuthConfig`.
- The main secret names are `AUTH_DEMO_*`.

## References Removed

Physically removed from the main project:

- `Kerberos/`
- `Seguridad/`
- `Chat/`
- `DistribucionClaves/`

Also removed versionable generated artifacts:

- `build/`
- `target/`
- `*.class`
- `*.ser`
- `sources.txt`, if it existed

Phase 9 also removed:

- `auth-transport/src/main/java/com/portfolio/auth/transport/javaio/`
- `auth-transport/src/test/java/com/portfolio/auth/transport/javaio/`
- `auth-transport/src/main/java/com/portfolio/auth/transport/legacy/`
- `auth-transport/src/test/java/com/portfolio/auth/transport/legacy/`

## Conclusion

`modular runtime is legacy-independent`.

The physical legacy code was removed from the main project after compile, Maven
tests, and textual audit passed. Phase 9 also removed the internal adapters that
preserved Java serialization or historical mappers. Phase 11 removed the
historical configuration aliases and validated the Gateway with real WebSocket
E2E.
