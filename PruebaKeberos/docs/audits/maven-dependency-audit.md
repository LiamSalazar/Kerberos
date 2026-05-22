# Maven Dependency Audit

Fecha: 2026-05-22

Comandos ejecutados:

- `mvn dependency:tree`
- `mvn -q -DskipTests compile`
- `mvn test`
- `mvn -pl auth-storage-sqlite -am test`
- `mvn -pl auth-websocket-gateway -am test`

Nota: un intento inicial de `dependency:tree` con `-DoutputFile` fallo por
quoting de PowerShell. El comando requerido `mvn dependency:tree` se ejecuto
despues y termino en `BUILD SUCCESS`.

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
- `org.slf4j:slf4j-api` llega transitivamente por SQLite JDBC y Java-WebSocket.

Las versiones principales siguen centralizadas en el `dependencyManagement` del
POM raiz.

## Dependencias Agregadas

- Se agrego dependencia interna `auth-storage-sqlite` en
  `auth-websocket-gateway` para poder instanciar `SQLiteAuditRepository` y
  aplicar migraciones cuando `AUTH_STORAGE_MODE=sqlite`.

No se agregaron dependencias externas nuevas durante Fase 15.

## Dependencias Eliminadas

No se eliminaron dependencias. No se identificaron duplicados o dependencias no
usadas con claridad suficiente para retirarlas sin riesgo.

## Riesgos Detectados

- `sqlite-jdbc` y `Java-WebSocket` traen `slf4j-api` transitivo; la suite muestra
  advertencias por ausencia de provider/binding SLF4J. Es ruido conocido y no
  afecta el resultado funcional de pruebas, pero conviene decidir un binding o
  excluir logging transitorio en una fase futura.
- `auth-as`, `auth-tgs` y `auth-service` dependen de `auth-storage-sqlite` para
  soportar `AUTH_STORAGE_MODE=sqlite`; se mantiene el modo `memory` como default.
- `auth-websocket-gateway` ahora depende de `auth-storage-sqlite` para auditoria
  persistente opcional. No acopla WebSockets dentro de AS/TGS/Service.

## Conclusion

El estado Maven queda consistente para Fase 15. Las referencias internas usan
`groupId=com.portfolio.auth`, `artifactId` correcto y `${project.version}`. Las
dependencias de test permanecen en scope `test`. No hay ciclos Maven observados.
