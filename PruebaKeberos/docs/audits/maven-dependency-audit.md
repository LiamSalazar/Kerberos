# Maven Dependency Audit

Fecha: 2026-05-22

Comando ejecutado para esta revision:

- `mvn dependency:tree`

Resultado: `BUILD SUCCESS`.

## Modulos Revisados

- `auth-core`
- `auth-crypto`
- `auth-transport`
- `auth-storage-sqlite`
- `auth-as`
- `auth-tgs`
- `auth-service`
- `auth-client-sdk`
- `auth-websocket-gateway`
- `docs`

## Dependencias Internas

- `auth-core`: sin dependencias internas.
- `auth-crypto`: `auth-core`.
- `auth-transport`: `auth-core`, `auth-crypto`.
- `auth-storage-sqlite`: `auth-core`.
- `auth-as`: `auth-core`, `auth-crypto`, `auth-transport`, `auth-storage-sqlite`.
- `auth-tgs`: `auth-core`, `auth-crypto`, `auth-transport`, `auth-storage-sqlite`.
- `auth-service`: `auth-core`, `auth-crypto`, `auth-transport`, `auth-storage-sqlite`.
- `auth-client-sdk`: `auth-core`, `auth-crypto`, `auth-transport`; AS/TGS/Service/SQLite solo en test.
- `auth-websocket-gateway`: `auth-core`, `auth-crypto`, `auth-transport`, `auth-client-sdk`, `auth-storage-sqlite`; AS/TGS/Service solo en test.

No se detectaron ciclos Maven entre modulos.

## Dependencias Externas

- `org.junit.jupiter:junit-jupiter:5.10.2` en scope `test`.
- `org.xerial:sqlite-jdbc:3.45.3.0` en `auth-storage-sqlite`.
- `org.java-websocket:Java-WebSocket:1.5.6` en `auth-websocket-gateway`.
- `org.slf4j:slf4j-simple:1.7.36` en `auth-storage-sqlite` con scope `runtime`.
- `org.slf4j:slf4j-simple:2.0.6` en `auth-websocket-gateway` con scope `runtime`.

## Dependencias Agregadas

- `slf4j-simple:1.7.36` se agrego para cubrir el `slf4j-api:1.7.36`
  transitivo de `sqlite-jdbc` y limpiar warnings en storage, AS, TGS, Service y
  pruebas que usan SQLite.
- `slf4j-simple:2.0.6` se agrego al Gateway para cubrir el `slf4j-api:2.0.6`
  transitivo de `Java-WebSocket`.
- `auth-websocket-gateway` excluye el binding SLF4J 1.7 transitivo desde
  `auth-storage-sqlite` para evitar mezclar providers SLF4J en el runtime del
  Gateway.

## Validacion De Scope

- Dependencias JUnit permanecen en `test`.
- `sqlite-jdbc` es compile/runtime solo donde se necesita modo SQLite.
- Los bindings SLF4J se mantienen en `runtime`; no cambian codigo de negocio ni
  imprimen secretos.
- No se agrego Spring Boot, PostgreSQL ni ORM.

## Riesgos Detectados

- El proyecto usa dos lineas SLF4J porque `sqlite-jdbc` trae API 1.7 y
  `Java-WebSocket` trae API 2.0. El Gateway excluye el binding 1.7 para que su
  runtime use el provider 2.0.
- AS/TGS/Service dependen de `auth-storage-sqlite` para soportar
  `AUTH_STORAGE_MODE=sqlite`; `AUTH_STORAGE_MODE=memory` sigue siendo default.
- `auth-websocket-gateway` depende de `auth-storage-sqlite` para auditoria
  persistente opcional. No acopla WebSockets dentro de AS/TGS/Service.

## Conclusion

El estado Maven queda consistente para Fase 16. Las referencias internas usan
`groupId=com.portfolio.auth`, `artifactId` correcto y `${project.version}`. Las
dependencias de test permanecen en scope `test`. No hay ciclos Maven observados.
