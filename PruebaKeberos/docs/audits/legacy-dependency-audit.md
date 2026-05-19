# Legacy Dependency Audit

Fecha de ejecucion: 2026-05-19

Objetivo: confirmar si los modulos `auth-*` dependen de los paquetes legacy
`Kerberos/` o `Seguridad/` antes de considerar una eliminacion controlada.

## Comandos Ejecutados

```powershell
rg -n "package Kerberos|package Seguridad|\bKerberos\b|\bSeguridad\b|AESUtils|SealedObject|ObjectInputStream|ObjectOutputStream|HashMap<String,\s*Object>|HashMap<String,Object>" auth-as\src auth-tgs\src auth-service\src auth-client-sdk\src auth-core\src auth-crypto\src auth-transport\src
```

```powershell
rg -n "import Kerberos|import Seguridad|Kerberos\.|Seguridad\." auth-as\src auth-tgs\src auth-service\src auth-client-sdk\src auth-core\src auth-crypto\src auth-transport\src
```

## Referencias Encontradas

- No se encontraron imports ni referencias directas desde `auth-*` hacia
  `Kerberos.*` o `Seguridad.*`.
- `auth-crypto` menciona `AESUtils` solamente en comentarios para aclarar que
  AES-GCM modular no reemplaza automaticamente la compatibilidad legacy.
- `auth-transport/src/main/java/com/portfolio/auth/transport/javaio` contiene
  `ObjectInputStream` y `ObjectOutputStream` como adaptador explicito de
  compatibilidad, no usado por `AuthenticationServerApp`,
  `TicketGrantingServerApp`, `ProtectedServiceApp` ni `ClientCli`.
- `auth-transport/src/main/java/com/portfolio/auth/transport/legacy` contiene
  `HashMap<String, Object>` en mappers legacy para conservar conversiones
  historicas. La ruta modular principal usa DTOs, `ProtocolEnvelope`, JSON y
  `CryptoEnvelope`.

## Referencias Eliminadas

No se eliminaron referencias directas a `Kerberos/` o `Seguridad/` porque la
busqueda en `auth-*` no encontro dependencias de paquete hacia esas carpetas.

## Referencias Justificadas

- `auth-transport/legacy`: mappers de compatibilidad, cubiertos por pruebas, no
  forman parte del runtime modular principal.
- `auth-transport/javaio`: helper de compatibilidad para serializacion Java
  legacy; no es usado por el flujo modular JSON/TCP.
- Comentarios sobre `AESUtils`: documentan limites del legacy y evitan afirmar
  que AES-GCM migro toda la demo historica.

## Conclusion

`modular runtime is legacy-independent` respecto a imports y dependencias de
paquete hacia `Kerberos/` y `Seguridad/`.

La eliminacion fisica de `Kerberos/` y `Seguridad/` no se ejecuto en esta fase
porque el gate obligatorio de Maven no pudo verificarse en este entorno:
`mvn` no esta disponible en el PATH.
