# Security Hardening Roadmap

## Objetivo

Endurecer el proyecto para presentarlo como un proyecto serio y bien cuidado,
sin venderlo como una plataforma de seguridad critica ni forzar una reescritura
total del flujo actual en `Kerberos/`.

Esta hoja de ruta se basa en el estado real del codigo hoy:

- `Kerberos/AESUtils.java` usa `AES/CBC/PKCS5Padding`, un salt fijo y un IV fijo.
- `Kerberos/AuthenticationServer.java`, `Kerberos/TicketGrantingServer.java`,
  `Kerberos/ServiceServer.java` y `Kerberos/Client.java` contienen secretos
  hardcodeados.
- `Seguridad/Comunicacion.java` usa `ObjectInputStream` y `ObjectOutputStream`
  directamente sobre sockets.
- El control actual de replay depende solo de timestamps y validez temporal del
  ticket; no existe una cache de autenticadores ya vistos.
- El monorepo ya contiene DTOs mas sanos en `auth-core/` con campos como
  `requestId`, `issuedAt` y `expiresAt`, lo cual es una buena base para el
  trabajo futuro.

## Riesgos actuales

### 1. IV fijo en AES/CBC

El metodo `generateIv()` genera bytes aleatorios y luego los reemplaza por un
vector constante:

- rompe la seguridad esperada de CBC
- hace predecibles los cifrados del mismo contenido bajo la misma clave
- impide una migracion limpia a autenticacion criptografica moderna

### 2. Secretos embebidos en el codigo

Hoy existen passwords y claves directamente en clases de negocio:

- `ContraseniaCliente`
- `contraseña_C-TGS`
- `contraseñaTGS`
- `contraseñaServidor`
- mapas `BDD_*` embebidos en `AuthenticationServer`

Esto dificulta rotacion, separacion de ambientes y pruebas controladas.

### 3. Serializacion Java por sockets

`Seguridad/Comunicacion.java` recibe y envia objetos arbitrarios con
`ObjectInputStream` y `ObjectOutputStream`.

Aunque el proyecto sea demo, esta decision sigue siendo el mayor riesgo
estructural:

- superficie de deserializacion insegura
- fuerte acoplamiento entre cliente y servidor
- formato opaco y dificil de validar
- muy poca compatibilidad para evolucionar el protocolo

### 4. Replay defense insuficiente

El servicio valida:

- coincidencia `clientId`
- coincidencia IP
- expiracion del ticket

Pero no rechaza un autenticador reutilizado dentro de la ventana valida del
ticket. Si un atacante captura `Ticket-v` y `Autentificador-c`, puede intentar
reinyectarlos antes de expirar.

### 5. Ausencia de autenticacion de mensajes moderna

CBC protege confidencialidad, pero no es un AEAD. En este proyecto se esta
confiando en `SealedObject` y en el exito del descifrado como si fuera
proteccion integral del mensaje.

## Cambios minimos vs. cambios ideales

### Cambios minimos

Estos cambios mejoran bastante la postura de seguridad sin reescribir todo el
proyecto:

1. Sustituir IV fijo por IV aleatorio por mensaje.
2. Migrar de `AES/CBC/PKCS5Padding` a `AES/GCM/NoPadding`.
3. Introducir un sobre criptografico propio que transporte:
   - version
   - algoritmo
   - iv/nonce
   - ciphertext
4. Externalizar secretos a configuracion fuera del codigo.
5. Agregar replay cache en TGS y Service.
6. Validar ventana de clock skew para autenticadores.
7. Limitar clases deserializables con `ObjectInputFilter` mientras exista
   serializacion Java.
8. Reducir logging de datos sensibles.

### Cambios ideales

Estos son los cambios correctos a mediano plazo si el proyecto quiere verse mas
maduro tecnicamente:

1. Eliminar serializacion Java del transporte.
2. Mover el protocolo a DTOs explicitamente versionados usando `auth-core/`.
3. Usar JSON o CBOR con validacion estricta de esquema.
4. Separar `auth-crypto`, `auth-transport`, `auth-as`, `auth-tgs`,
   `auth-service` y `auth-client-sdk` como implementacion real, no solo
   estructura de monorepo.
5. Introducir ids de ticket y autenticador (`ticketId`, `requestId`).
6. Añadir rotacion de secretos y soporte por ambiente.
7. Incorporar TLS si el proyecto quiere proteger transporte ademas del payload.

## Recomendacion de fases

### Fase 1: endurecimiento inmediato sin reescritura total

Alcance recomendado:

- corregir IV fijo
- migrar a AES-GCM
- agregar replay cache
- mover secretos a configuracion
- endurecer deserializacion con allowlist
- documentar limitaciones pendientes

Esta fase mantiene:

- sockets
- `SealedObject` o un contenedor transitorio similar
- flujo AS -> TGS -> Service actual

### Fase 2: saneamiento de protocolo

Alcance recomendado:

- reemplazar `HashMap<String, Object>` por DTOs
- dejar de intercambiar objetos Java arbitrarios
- introducir `requestId`, `ticketId`, `issuedAt`, `expiresAt`
- serializar en formato estable

### Fase 3: arquitectura mas limpia

Alcance recomendado:

- usar realmente los modulos `auth-*`
- aislar crypto, transporte y dominio
- pruebas automatizadas de compatibilidad y replay

## Migracion propuesta: AES/CBC -> AES-GCM

### Por que conviene AES-GCM aqui

`AES/GCM/NoPadding` da confidencialidad e integridad autenticada en una sola
operacion. Para este proyecto eso resuelve mejor el problema que CBC porque:

- elimina el esquema actual dependiente de IV fijo
- evita confiar en descifrado exitoso como señal de validez
- facilita versionar un payload autocontenido con nonce + ciphertext

### Estrategia de migracion minima

No conviene tocar todo en un solo salto. La migracion recomendada es:

1. Crear un contenedor serializable, por ejemplo `EncryptedEnvelope`, con:
   - `version`
   - `algorithm`
   - `iv`
   - `ciphertext`
2. Agregar en `AESUtils` nuevos metodos:
   - `encryptGcm(Serializable object, SecretKey key)`
   - `decryptGcm(EncryptedEnvelope envelope, SecretKey key)`
3. Mantener temporalmente compatibilidad de lectura con CBC solo para no romper
   pruebas de transicion.
4. Cambiar primero los intercambios mas expuestos:
   - respuesta AS -> cliente
   - respuesta TGS -> cliente
   - peticion cliente -> servicio
5. Una vez validado GCM en todos los saltos, eliminar CBC.

### Parametros recomendados

- algoritmo: `AES/GCM/NoPadding`
- nonce por mensaje: 12 bytes aleatorios
- tag: 128 bits
- clave AES: 256 bits si el entorno ya lo soporta
- AAD opcional:
  - `protocolVersion`
  - `messageType`
  - `source`
  - `destination`

### Compatibilidad y alcance

Lo importante es entender que el cambio minimo real no es solo cambiar la
cadena `AES/CBC/PKCS5Padding` por `AES/GCM/NoPadding`. Tambien hay que dejar de
asumir un IV implcito y constante. El nonce debe viajar con cada mensaje.

Si no se quiere reescribir el transporte todavia, ese nonce puede viajar dentro
de un objeto serializable intermedio.

## Replay cache: estrategia recomendada

### Objetivo

Rechazar autenticadores repetidos dentro de la ventana de validez, incluso si el
ticket aun no expira.

### Donde aplicar la cache

- en `TicketGrantingServer` para autenticadores presentados junto con
  `Ticket-tgs`
- en `ServiceServer` para autenticadores presentados junto con `Ticket-v`

### Clave de cache recomendada

Mientras el protocolo actual no tenga `ticketId` ni `requestId`, la clave puede
derivarse de:

- `clientId`
- `serviceId` o `tgsId`
- timestamp del autenticador
- hash del ticket cifrado recibido

Formula conceptual:

`scope + ":" + clientId + ":" + authenticatorTimestamp + ":" + sha256(ticketBytes)`

Cuando exista `ticketId`, conviene pasar a:

`scope + ":" + ticketId + ":" + authenticatorIssuedAt`

### Politica de expiracion

TTL recomendado para cada entrada:

- `min(ticketExpiresAt, authenticatorIssuedAt + allowedSkew + replayWindow)`

Valores iniciales razonables para demo seria:

- `allowedSkew`: 60 a 120 segundos
- `replayWindow`: 2 a 5 minutos

### Regla de validacion

Orden recomendado:

1. descifrar ticket
2. validar expiracion del ticket
3. descifrar autenticador
4. validar identidad e IP si se decide mantener IP binding
5. validar clock skew del timestamp del autenticador
6. consultar replay cache
7. si ya existe entrada activa: rechazar
8. si no existe: registrar y continuar

### Implementacion minima

Sin meter infraestructura externa, basta con una cache en memoria por proceso:

- `ConcurrentHashMap<String, Instant>`
- limpieza periodica de expirados
- uso de reloj injectable en el futuro si se quieren pruebas deterministas

Si luego hay varias instancias del servicio, la cache debera migrar a un backend
compartido como Redis. Para el estado actual del proyecto, memoria local es
aceptable y suficiente.

## Externalizacion de secretos y configuracion

### Que externalizar

Minimo:

- secreto del cliente de demo
- secreto AS <-> TGS
- secreto TGS <-> Service
- seed o salt configurable
- puertos e IPs de AS, TGS y Service
- duracion de tickets
- ventana de replay y clock skew

### Estrategia recomendada

### Corto plazo

Usar `Properties` o variables de entorno, con un cargador simple:

- archivo local no versionado, por ejemplo `config/auth-dev.properties`
- soporte de override por variables de entorno

Prioridad de lectura:

1. variable de entorno
2. archivo properties local
3. valor por defecto solo para demos locales controladas

### Mediano plazo

Crear una clase central de configuracion, por ejemplo `AuthConfig`, para que el
codigo deje de leer literales dispersos.

### Ejemplo de claves de configuracion

- `auth.client.1.secret`
- `auth.tgs.1.secret`
- `auth.service.1.secret`
- `auth.session.clientTgs.secret`
- `auth.session.clientService.secret`
- `auth.ticket.ttl.minutes`
- `auth.replay.allowedSkew.seconds`
- `auth.replay.window.seconds`
- `auth.crypto.algorithm`
- `auth.crypto.pbkdf2.salt`
- `auth.as.port`
- `auth.tgs.port`
- `auth.service.port`

## Que se puede corregir ya

Estas correcciones son razonables en la base actual y no requieren una
reescritura total:

1. Dejar de usar IV fijo.
2. Introducir AES-GCM con nonce por mensaje.
3. Crear un `EncryptedEnvelope` serializable para transportar nonce y
   ciphertext.
4. Externalizar secretos a properties/env vars.
5. Implementar replay cache local en memoria.
6. Validar skew y rechazar timestamps viejos o muy adelantados.
7. Añadir `ObjectInputFilter` para permitir solo clases esperadas.
8. Quitar logs que imprimen tickets, autenticadores o claves derivadas.

## Que debe quedar como trabajo futuro documentado

Estas tareas son importantes, pero ya entran en evolucion de arquitectura y no
son necesarias para el siguiente salto de madurez:

1. Eliminar por completo `ObjectInputStream` y `SealedObject`.
2. Reemplazar `HashMap<String, Object>` por DTOs versionados.
3. Mover la implementacion real a los modulos `auth-*`.
4. Introducir `ticketId`, `requestId`, `nonce` y codigos de error tipados.
5. Soportar almacenamiento distribuido para replay cache.
6. Incorporar TLS y autenticacion mutua si el alcance del proyecto crece.
7. Diseñar rotacion formal de secretos y multiples ambientes.

## Prioridad sugerida

Si hubiera que ejecutar esto en el menor numero de cambios razonables, el orden
recomendado es:

1. externalizar secretos y parametros
2. introducir `EncryptedEnvelope` con AES-GCM
3. agregar replay cache y validacion de skew
4. endurecer deserializacion con allowlist
5. documentar como deuda tecnica la salida de serializacion Java

## Posicionamiento honesto del proyecto

Despues de la Fase 1, el proyecto ya no se veria como un demo ingenuo:

- deja atras IV fijo y secretos hardcodeados
- tiene integridad autenticada con GCM
- cuenta con defensa practica contra replay
- reduce el riesgo de deserializacion con controles temporales

Pero aun deberia presentarse honestamente como:

- proyecto academico o portfolio endurecido
- prototipo serio
- implementacion inspirada en Kerberos

Y no como:

- sistema listo para produccion critica
- referencia de seguridad enterprise

## Siguiente paso recomendado

El siguiente cambio con mejor relacion impacto/esfuerzo es implementar la Fase 1
sin romper el flujo actual:

1. `AuthConfig` para secretos y puertos
2. `EncryptedEnvelope` + `AES/GCM/NoPadding`
3. replay cache en TGS y Service
4. filtro de deserializacion por allowlist
