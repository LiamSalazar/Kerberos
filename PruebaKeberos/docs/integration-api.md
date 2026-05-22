# Integration API

Este documento resume como conectar repositorios o servicios reales sin cambiar
el flujo modular principal.

## Repositorios De Identidad

`auth-core` define `PrincipalRepository`:

```java
Optional<String> clientSecret(String clientId);
Optional<String> ticketGrantingServerSecret(String tgsId);
```

`auth-as` depende de esta interfaz. Para integrar una fuente real de clientes,
crea una implementacion que resuelva secretos por `clientId` y secretos del TGS
por `tgsId`.

Implementaciones actuales:

- `InMemoryPrincipalRepository` para demo local.
- `SQLitePrincipalRepository` para SQLite local.

## Registro De Servicios

`auth-core` define `ServiceRegistry`:

```java
Optional<String> ticketGrantingServerSecret(String tgsId);
Optional<String> serviceSecret(String serviceId);
```

`auth-tgs` y `auth-service` dependen de esta interfaz. Para integrar un registro
real, implementa busqueda de secretos del TGS y de servicios por id.

Implementaciones actuales:

- `InMemoryServiceRegistry` para demo local.
- `SQLiteServiceRegistry` para SQLite local.

## Recurso Protegido

`auth-service` define `ProtectedResource`:

```java
String getServiceId();
ProtectedServiceResponse execute(ProtectedServiceRequest request);
```

`ProtectedServiceHandler` valida ticket, autenticador, replay y expiracion antes
de invocar el recurso. Una aplicacion externa debe implementar `ProtectedResource`
para encapsular la accion protegida real.

Implementacion actual:

- `DemoProtectedResource`, que devuelve una respuesta didactica local.

## Registrar Cliente Y Servicio En SQLite

Inicializa la base:

```cmd
scripts\init-sqlite-demo.bat --db data\auth-demo.sqlite
```

Luego agrega registros en:

- `principals` para clientes y TGS;
- `services` para servicios protegidos.

El esquema esta en `scripts/sqlite/schema.sql`.

## Seleccionar Storage

Modo memoria:

```text
AUTH_STORAGE_MODE=memory
```

Modo SQLite:

```text
AUTH_STORAGE_MODE=sqlite
AUTH_SQLITE_PATH=data/auth-demo.sqlite
```

## Limites Actuales

- No hay API HTTP de administracion para registrar clientes o servicios.
- No hay migrador de schema ni rotacion de secretos.
- No hay vault externo.
- `HttpProtectedResource` no se implemento en esta fase para evitar introducir
  acoplamiento prematuro o dependencias externas.
- El sistema no es production-ready.
