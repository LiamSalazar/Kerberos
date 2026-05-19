# Legacy Dependency Audit

Fecha de ejecucion: 2026-05-19

Objetivo: confirmar si los modulos `auth-*` dependen de los paquetes
historicos antes de retirar las carpetas legacy del proyecto principal.

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

## Resultado Maven

- Compile: paso.
- Test: paso.
- Total reportado por Maven: 52 tests, 0 failures, 0 errors, 0 skipped.

## Referencias Encontradas

- No se encontraron imports ni referencias directas desde `auth-*` hacia los
  paquetes historicos.
- Persisten `ObjectInputStream` y `ObjectOutputStream` en
  `auth-transport/javaio` como adaptador historico interno. No apunta a las
  carpetas retiradas y no es la ruta modular principal.

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

## Conclusion

`modular runtime is legacy-independent`.

El legacy fisico fue eliminado del proyecto principal despues de pasar compile,
tests Maven y auditoria textual.
