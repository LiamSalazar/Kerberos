# Legacy Removal Blockers

Date: 2026-05-19

Final status: no current blockers. Physical legacy removal was resolved, and
the technical Phase 9 follow-up was also closed.

## Previous Blockers

- Physical removal was conditional on compile, Maven tests, and textual audit
  without dependencies on historical packages.
- The later technical follow-up required removing `auth-transport/javaio` and
  `auth-transport/legacy`.

## Resolution

- `mvn -q -DskipTests compile` passed.
- `mvn test` passed with 52 tests, 0 failures, 0 errors, 0 skipped.
- The textual audit found no imports from `auth-*` toward historical packages.
- The physical legacy code was removed from the main project.
- `auth-transport/javaio` and `auth-transport/legacy` were removed.
- `auth-websocket-gateway` compiles and is part of the Maven reactor.

## Follow-Up

This file remains as a historical record. In Phase 9,
`auth-transport/javaio` and `auth-transport/legacy` adapters were removed. In
Phase 11, historical configuration aliases were removed; there are no current
legacy blockers.
