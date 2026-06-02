# Maven Dependency Audit

Date: 2026-05-24

Command executed for this review:

- `mvn dependency:tree`

Result: `BUILD SUCCESS`.

## Reviewed Modules

- `auth-core`
- `auth-crypto`
- `auth-transport`
- `auth-storage-sqlite`
- `auth-storage-postgres`
- `auth-as`
- `auth-tgs`
- `auth-service`
- `auth-client-sdk`
- `auth-websocket-gateway`
- `docs`

## Internal Dependencies

- `auth-core`: no internal dependencies.
- `auth-crypto`: `auth-core`.
- `auth-transport`: `auth-core`, `auth-crypto`.
- `auth-storage-sqlite`: `auth-core`.
- `auth-storage-postgres`: `auth-core`.
- `auth-as`: `auth-core`, `auth-crypto`, `auth-transport`, `auth-storage-sqlite`, `auth-storage-postgres`.
- `auth-tgs`: `auth-core`, `auth-crypto`, `auth-transport`, `auth-storage-sqlite`, `auth-storage-postgres`.
- `auth-service`: `auth-core`, `auth-crypto`, `auth-transport`, `auth-storage-sqlite`, `auth-storage-postgres`.
- `auth-client-sdk`: `auth-core`, `auth-crypto`, `auth-transport`; AS/TGS/Service/SQLite only in tests.
- `auth-websocket-gateway`: `auth-core`, `auth-crypto`, `auth-transport`, `auth-client-sdk`, `auth-storage-sqlite`, `auth-storage-postgres`; AS/TGS/Service only in tests.

No Maven cycles were detected between modules.

## External Dependencies

- `org.junit.jupiter:junit-jupiter:5.10.2` in `test` scope.
- `org.xerial:sqlite-jdbc:3.45.3.0` in `auth-storage-sqlite`.
- `org.postgresql:postgresql:42.7.3` in `auth-storage-postgres`.
- `software.amazon.awssdk:secretsmanager:2.25.60` in `auth-core` through AWS SDK v2 BOM.
- `org.java-websocket:Java-WebSocket:1.5.6` in `auth-websocket-gateway`.
- `org.slf4j:slf4j-simple:1.7.36` in `auth-storage-sqlite` with `runtime` scope.
- `org.slf4j:slf4j-simple:2.0.6` in `auth-websocket-gateway` with `runtime` scope.

## Added Dependencies

- `slf4j-simple:1.7.36` was added to cover the transitive
  `slf4j-api:1.7.36` from `sqlite-jdbc` and clean warnings in storage, AS, TGS,
  Service, and tests that use SQLite.
- `slf4j-simple:2.0.6` was added to the Gateway to cover the transitive
  `slf4j-api:2.0.6` from `Java-WebSocket`.
- `auth-websocket-gateway` excludes the transitive SLF4J 1.7 binding from
  `auth-storage-sqlite` to avoid mixing SLF4J providers in the Gateway runtime.
- `postgresql:42.7.3` was added to implement PostgreSQL JDBC repositories
  without a heavy ORM in `auth-storage-postgres`.
- AWS SDK v2 `secretsmanager` was added for `AwsSecretsManagerProvider`; normal
  tests use a fake resolver and do not call real AWS.

## Scope Validation

- JUnit dependencies remain in `test`.
- `sqlite-jdbc` is compile/runtime only where SQLite mode is needed.
- SLF4J bindings remain in `runtime`; they do not change business code or print
  secrets.
- Spring Boot and ORM were not added.

## Detected Risks

- The project uses two SLF4J lines because `sqlite-jdbc` brings API 1.7 and
  `Java-WebSocket` brings API 2.0. The Gateway excludes the 1.7 binding so its
  runtime uses the 2.0 provider.
- AS/TGS/Service depend on `auth-storage-sqlite` to support
  `AUTH_STORAGE_MODE=sqlite` and on `auth-storage-postgres` for
  `AUTH_STORAGE_MODE=postgres`; `AUTH_STORAGE_MODE=memory` remains the default.
- `auth-websocket-gateway` depends on `auth-storage-sqlite` and
  `auth-storage-postgres` for optional persistent audit/sessions. It does not
  couple WebSockets inside AS/TGS/Service.

## Conclusion

The Maven state is consistent for Phase 20. New dependencies are limited to
PostgreSQL JDBC and AWS SDK v2 Secrets Manager. Internal references use
`groupId=com.portfolio.auth`, the correct `artifactId`, and
`${project.version}`. Test dependencies remain in `test` scope. No Maven cycles
were observed.
