# Kerberos-Inspired Modular Authentication Demo

Proyecto Java de portafolio que implementa desde cero un flujo de autenticacion
distribuida inspirado en Kerberos 4, centrado ahora en una arquitectura modular
propia.

Este repositorio no es MIT Kerberos oficial y no debe presentarse como un
sistema listo para produccion critica. Es una pieza de ingenieria aplicada para
mostrar arquitectura distribuida, diseno de protocolo, hardening incremental,
pruebas y ejecucion local reproducible.

## Estado Actual

Fase actual: **Fase 11: prueba end-to-end real del WebSocket Gateway y contrato estable para frontend**.

| Area | Rol | Estado |
| --- | --- | --- |
| `auth-core/` | DTOs del protocolo, configuracion y replay cache | Activo |
| `auth-crypto/` | AES-GCM, `CryptoEnvelope`, derivacion y claves de sesion | Activo |
| `auth-transport/` | `ProtocolEnvelope`, JSON/TCP y DTOs seguros | Activo |
| `auth-as/` | Authentication Server modular | Ejecutable |
| `auth-tgs/` | Ticket Granting Server modular | Ejecutable |
| `auth-service/` | Servicio protegido modular | Ejecutable |
| `auth-client-sdk/` | Cliente modular, CLI y audit runner | Ejecutable |
| `auth-websocket-gateway/` | Gateway WebSocket separado para futuras integraciones web | Ejecutable |
| `docs/` | Documentacion tecnica y auditorias | Activa |

El codigo legacy fisico fue retirado del proyecto principal. Su existencia se
resume en [docs/legacy-summary.md](docs/legacy-summary.md), sin conservarlo
como ruta ejecutable actual.

## Arquitectura Modular

Vista simplificada:

```mermaid
sequenceDiagram
    participant C as Client
    participant AS as Authentication Server
    participant TGS as Ticket Granting Server
    participant S as Service Server

    C->>AS: AS_REQUEST JSON (AsRequest)
    AS-->>C: AS_RESPONSE JSON (CryptoEnvelope<SecureAsResponse>)
    C->>TGS: TGS_REQUEST JSON (SecureTgsRequest)
    TGS-->>C: TGS_RESPONSE JSON (CryptoEnvelope<SecureTgsResponse>)
    C->>S: SERVICE_REQUEST JSON (SecureServiceRequest)
    S-->>C: SERVICE_RESPONSE JSON (CryptoEnvelope<ServiceResponse>)
```

La ruta principal usa DTOs tipados, JSON/TCP, AES-GCM con `CryptoEnvelope`,
replay cache, configuracion demo/strict y auditoria reproducible.

El WebSocket Gateway no reemplaza AS, TGS ni Service. Expone una capa de
integracion que recibe mensajes WebSocket y ejecuta el flujo modular existente
mediante `AuthClient`.

## Requisitos

Consulta tambien [requirements.txt](requirements.txt).

- Java 17 o superior.
- Maven 3.9+.
- Git.
- Windows, Linux o macOS con terminal.
- Docker no es requisito en esta fase.

## Compilar Y Probar

Desde esta carpeta:

```bash
mvn -q -DskipTests compile
mvn test
```

En la verificacion de Fase 11 ambos comandos pasaron. Tambien paso el gate
especifico del gateway: `mvn -pl auth-websocket-gateway -am test`.

## Ejecutar Sin Docker

En Windows, abre tres terminales para servidores y una para cliente:

```cmd
scripts\run-as.bat
```

```cmd
scripts\run-tgs.bat
```

```cmd
scripts\run-service.bat
```

```cmd
scripts\run-client.bat
```

Los scripts compilan con Maven antes de ejecutar las clases modulares.

En Linux/macOS tambien existen scripts equivalentes:

```bash
scripts/run-as.sh
scripts/run-tgs.sh
scripts/run-service.sh
scripts/run-client.sh
```

## WebSocket Gateway

Primero levanta AS, TGS y Service. Despues:

```cmd
scripts\run-websocket-gateway.bat
```

En Linux/macOS:

```bash
scripts/run-websocket-gateway.sh
```

Por defecto escucha en `127.0.0.1:2800`. Variables:

- `AUTH_WS_HOST`
- `AUTH_WS_PORT`

Mensaje minimo:

```json
{"type":"START_AUTH_FLOW","requestId":"manual-1","clientId":"1","serviceId":"1"}
```

## Auditoria Modular

Con AS, TGS y Service levantados:

```cmd
scripts\run-audit.bat --iterations 5
```

El runner genera evidencia en:

- `docs/audits/latest-run.md`
- `docs/audits/latest-run.json`

La auditoria de independencia del runtime modular esta en
[docs/audits/legacy-dependency-audit.md](docs/audits/legacy-dependency-audit.md).

## Configuracion

Variables comunes:

- `AUTH_MODE`: `demo`, `local` o `strict`.
- `AUTH_AS_PORT`
- `AUTH_TGS_PORT`
- `AUTH_SERVICE_PORT`
- `AUTH_DEMO_CLIENT_ID`
- `AUTH_DEMO_TGS_ID`
- `AUTH_DEMO_SERVICE_ID`
- `AUTH_TICKET_TTL_MINUTES`
- `AUTH_ALLOWED_SKEW_SECONDS`
- `AUTH_REPLAY_WINDOW_SECONDS`
- `AUTH_DEMO_CLIENT_SECRET`
- `AUTH_DEMO_CLIENT_TGS_KEY`
- `AUTH_DEMO_TGS_SECRET`
- `AUTH_DEMO_CLIENT_SERVICE_KEY`
- `AUTH_DEMO_SERVICE_SECRET`
- `AUTH_DEMO_PBKDF2_SALT`

Los nombres de secretos actuales usan `AUTH_DEMO_*`. En `AUTH_MODE=strict`,
estos valores deben definirse explicitamente y no pueden quedarse en defaults.

## CI

GitHub Actions vive en la raiz Git:

- `../.github/workflows/maven.yml`

El workflow usa `working-directory: PruebaKeberos` y ejecuta:

- `mvn -q -DskipTests compile`
- `mvn test`

## Limitaciones Actuales

- No es production-ready.
- No hay Docker ni frontend.
- El WebSocket Gateway existe como capa de integracion separada; no sustituye
  el runtime TCP modular.
- El replay cache es local por proceso.
- No hay TLS ni autenticacion mutua de transporte.
- El codec JSON es propio y acotado a los DTOs del proyecto.
- El gateway WebSocket ya tiene E2E real, pero no hay frontend todavia.

## Roadmap

1. Usar el contrato WebSocket desde un frontend futuro sin acoplarlo a AS/TGS/Service.
2. Evaluar un parser JSON mantenido si el codec propio crece fuera de su alcance
   acotado.
3. Agregar TLS o una capa de transporte autenticada.
4. Introducir Docker y Docker Compose solo en una fase futura de despliegue.
5. Crear frontend solo cuando exista una fase especifica.

Mas detalle:

- [docs/execution-guide.md](docs/execution-guide.md)
- [docs/architecture.md](docs/architecture.md)
- [docs/protocol-flow.md](docs/protocol-flow.md)
- [docs/security-hardening-roadmap.md](docs/security-hardening-roadmap.md)
- [docs/websocket-gateway.md](docs/websocket-gateway.md)
- [docs/frontend-contract.md](docs/frontend-contract.md)
