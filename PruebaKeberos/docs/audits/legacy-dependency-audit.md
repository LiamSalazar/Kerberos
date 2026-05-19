# Legacy Dependency Audit

Fecha de ejecucion: 2026-05-19

Objetivo: confirmar si los modulos `auth-*` dependen de paquetes historicos,
Java serialization o adaptadores conceptuales legacy despues de Fase 9.

## Comandos Ejecutados

```powershell
& 'C:\Program Files\NetBeans-19\netbeans\java\maven\bin\mvn.cmd' -q -DskipTests compile
& 'C:\Program Files\NetBeans-19\netbeans\java\maven\bin\mvn.cmd' test
```

Busqueda de dependencias:

```powershell
rg -n "import Kerberos|import Seguridad|Kerberos\.|Seguridad\." auth-as\src auth-tgs\src auth-service\src auth-client-sdk\src auth-core\src auth-crypto\src auth-transport\src
```

```powershell
rg -n "\bKerberos[.]|\bSeguridad[.]|AESUtils|SealedObject|ObjectInputStream|ObjectOutputStream" auth-as\src auth-tgs\src auth-service\src auth-client-sdk\src auth-core\src auth-crypto\src auth-transport\src
```

Busqueda adicional de cierre Fase 9:

```powershell
rg -n "AUTH_LEGACY_|legacy[A-Z]|ObjectInputStream|ObjectOutputStream|SealedObject|AESUtils|com\.portfolio\.auth\.transport\.legacy|Legacy.*Mapper|transport\.legacy" auth-as auth-tgs auth-service auth-client-sdk auth-core auth-crypto auth-transport README.md requirements.txt docs AGENTS.md
```

## Resultado Maven

- Compile: paso.
- Test: paso.
- En Fase 10, `mvn -pl auth-websocket-gateway -am test` paso con 11 pruebas
  del gateway y sus dependencias.

## Referencias Encontradas

- No se encontraron imports ni referencias directas desde `auth-*` hacia los
  paquetes historicos.
- No quedan `ObjectInputStream`, `ObjectOutputStream`, `SealedObject` ni
  `AESUtils` en la ruta modular.
- `AUTH_LEGACY_*` existe solo como alias temporal de compatibilidad dentro de
  `AuthConfig` y documentacion asociada. Los nombres principales son
  `AUTH_DEMO_*`.

## Referencias Eliminadas

Se retiraron fisicamente del proyecto principal:

- `Kerberos/`
- `Seguridad/`
- `Chat/`
- `DistribucionClaves/`

Tambien se retiraron artefactos generados versionables:

- `build/`
- `target/`
- `*.class`
- `*.ser`
- `sources.txt`, si existia

En Fase 9 se retiraron tambien:

- `auth-transport/src/main/java/com/portfolio/auth/transport/javaio/`
- `auth-transport/src/test/java/com/portfolio/auth/transport/javaio/`
- `auth-transport/src/main/java/com/portfolio/auth/transport/legacy/`
- `auth-transport/src/test/java/com/portfolio/auth/transport/legacy/`

## Conclusion

`modular runtime is legacy-independent`.

El legacy fisico fue eliminado del proyecto principal despues de pasar compile,
tests Maven y auditoria textual. Fase 9 retiro tambien los adaptadores internos
que conservaban Java serialization o mappers historicos. El unico resto
deliberado es el alias temporal `AUTH_LEGACY_*` para compatibilidad de
configuracion.
