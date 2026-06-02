# Architecture

The main path is modular and lives in `auth-*`. It is not official MIT Kerberos
and must not be presented as ready for critical production use.

## Local Architecture

```mermaid
flowchart LR
    Browser["Browser demos\n5173 / 5174"] -->|WebSocket 2800| Gateway["auth-websocket-gateway"]
    Gateway --> AuthClient["auth-client-sdk\nAuthClient"]
    AuthClient -->|TCP/JSON 2000| AS["auth-as\nAuthenticationServerApp"]
    AuthClient -->|TCP/JSON 2001| TGS["auth-tgs\nTicketGrantingServerApp"]
    AuthClient -->|TCP/JSON 2002| Service["auth-service\nProtectedServiceApp"]
    Gateway --> Sessions["SessionRepository\nmemory/sqlite/postgres"]
    AS --> Principals["PrincipalRepository"]
    TGS --> Registry["ServiceRegistry"]
    Service --> Registry
```

## Docker Deployment

```mermaid
flowchart TB
    subgraph Public["auth-public network"]
        WebDemo["auth-web-demo:5173"]
        Sample["sample-login-app:5174"]
        Gateway["auth-websocket-gateway:2800/2801"]
    end
    subgraph Internal["auth-internal network"]
        AS["auth-as:2000/2900"]
        TGS["auth-tgs:2001/2901"]
        Service["auth-service:2002/2902"]
        SQLite["auth-sqlite-data volume"]
        Postgres["auth-postgres\nprofile postgres-local"]
    end
    WebDemo --> Gateway
    Sample --> Gateway
    Gateway --> AS
    Gateway --> TGS
    Gateway --> Service
    AS --> SQLite
    TGS --> SQLite
    Service --> SQLite
    Gateway --> SQLite
    AS -. postgres mode .-> Postgres
    TGS -. postgres mode .-> Postgres
    Service -. postgres mode .-> Postgres
    Gateway -. postgres mode .-> Postgres
```

## AWS Blueprint

```mermaid
flowchart TB
    Users["Users / Apps"] --> ALB["Public ALB\nHTTP/HTTPS/WSS"]
    ALB --> WebDemo["ECS auth-web-demo"]
    ALB --> Sample["ECS sample-login-app"]
    ALB --> Gateway["ECS auth-websocket-gateway"]
    subgraph PrivateSubnets["Private subnets"]
        Gateway --> AS["ECS auth-as"]
        Gateway --> TGS["ECS auth-tgs"]
        Gateway --> Service["ECS auth-service"]
        Gateway --> RDS["RDS PostgreSQL"]
        AS --> RDS
        TGS --> RDS
        Service --> RDS
    end
    Secrets["Secrets Manager"] --> AS
    Secrets --> TGS
    Secrets --> Service
    Secrets --> Gateway
    Logs["CloudWatch Logs"] --- Gateway
    Logs --- AS
    Logs --- TGS
    Logs --- Service
    ACM["ACM certificate"] --> ALB
    Discovery["Service Discovery"] --- AS
    Discovery --- TGS
    Discovery --- Service
```

AS/TGS/Service must not be public. The Gateway can be published through ALB/WSS
as the integration boundary.

## Maven Modules

```mermaid
flowchart LR
    Root["root pom.xml"] --> Core["auth-core"]
    Root --> Crypto["auth-crypto"]
    Root --> Transport["auth-transport"]
    Root --> AS["auth-as"]
    Root --> TGS["auth-tgs"]
    Root --> Service["auth-service"]
    Root --> Client["auth-client-sdk"]
    Root --> SQLite["auth-storage-sqlite"]
    Root --> Postgres["auth-storage-postgres"]
    Root --> Gateway["auth-websocket-gateway"]
    Crypto --> Core
    Transport --> Core
    Transport --> Crypto
    AS --> Core
    AS --> Transport
    TGS --> Core
    TGS --> Transport
    Service --> Core
    Service --> Transport
    Client --> Core
    Client --> Transport
    Gateway --> Client
    Gateway --> SQLite
    Gateway --> Postgres
```

## Main Class Diagram

```mermaid
classDiagram
    class AuthClient
    class AuthFlowRunner
    class WebSocketGatewayApp
    class AuthWebSocketServer
    class GatewayAuthFlowService
    class GatewaySessionService
    class SessionRepository
    class InMemorySessionRepository
    class SQLiteSessionRepository
    class PostgresSessionRepository
    class PrincipalRepository
    class ServiceRegistry
    class AuditRepository
    class AuthenticationServerApp
    class TicketGrantingServerApp
    class ProtectedServiceApp

    WebSocketGatewayApp --> AuthWebSocketServer
    WebSocketGatewayApp --> GatewayAuthFlowService
    WebSocketGatewayApp --> GatewaySessionService
    GatewayAuthFlowService --> AuthClient
    GatewayAuthFlowService --> AuditRepository
    GatewaySessionService --> SessionRepository
    SessionRepository <|.. InMemorySessionRepository
    SessionRepository <|.. SQLiteSessionRepository
    SessionRepository <|.. PostgresSessionRepository
    AuthFlowRunner --> AuthClient
    AuthenticationServerApp --> PrincipalRepository
    TicketGrantingServerApp --> ServiceRegistry
    ProtectedServiceApp --> ServiceRegistry
```

## Storage Modes

| Mode | Use |
| --- | --- |
| `AUTH_STORAGE_MODE=memory` | default demo mode without persistence. |
| `AUTH_STORAGE_MODE=sqlite` | verifiable local integration. |
| `AUTH_STORAGE_MODE=postgres` | cloud/RDS-ready mode and multiple Gateway instances. |

`AUTH_SESSION_STORAGE_MODE` can follow the storage mode or be configured
explicitly as `memory`, `sqlite`, or `postgres`.
