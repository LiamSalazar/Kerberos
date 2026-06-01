# Protocol Flow

El protocolo modular viaja por JSON/TCP entre AS, TGS y Service. El Gateway
WebSocket es una capa externa de integracion y no cambia el contrato interno.

## AS -> TGS -> Service

```mermaid
sequenceDiagram
    participant C as AuthClient
    participant AS as Authentication Server
    participant TGS as Ticket Granting Server
    participant S as Protected Service

    C->>AS: AS_REQUEST
    AS-->>C: AS_RESPONSE
    C->>TGS: TGS_REQUEST
    TGS-->>C: TGS_RESPONSE
    C->>S: SERVICE_REQUEST
    S-->>C: SERVICE_RESPONSE
```

## Gateway -> AuthClient -> AS/TGS/Service

```mermaid
sequenceDiagram
    participant App as Browser/App
    participant GW as WebSocket Gateway
    participant AC as AuthClient
    participant AS as AS
    participant TGS as TGS
    participant S as Service

    App->>GW: START_AUTH_FLOW
    GW->>AC: runFullFlow
    AC->>AS: AS_REQUEST
    AS-->>AC: AS_RESPONSE
    AC->>TGS: TGS_REQUEST
    TGS-->>AC: TGS_RESPONSE
    AC->>S: SERVICE_REQUEST
    S-->>AC: SERVICE_RESPONSE
    AC-->>GW: ServiceResponse
    GW-->>App: FLOW_RESULT
    App->>GW: VERIFY_SESSION
    GW-->>App: SESSION_VALID or SESSION_INVALID
```

## Complete Login

```mermaid
sequenceDiagram
    participant MF as MelodyFinder
    participant GW as Gateway
    participant Runtime as Modular Runtime
    participant Repo as SessionRepository

    MF->>GW: START_AUTH_FLOW(clientId, serviceId, requestId)
    GW->>Runtime: AS -> TGS -> Service
    Runtime-->>GW: access decision
    alt access granted
        GW->>Repo: save opaque session
        GW-->>MF: FLOW_RESULT(success=true, sessionId masked in UI)
        MF->>GW: VERIFY_SESSION(sessionId, clientId, serviceId)
        GW->>Repo: validate session
        GW-->>MF: SESSION_VALID
        MF->>MF: open protected dashboard
    else access denied
        GW-->>MF: FLOW_RESULT(success=false) or ERROR
        MF->>MF: keep access closed
    end
```

## Opaque Session

```mermaid
flowchart LR
    App["External app"] -->|VERIFY_SESSION| Gateway["Gateway"]
    Gateway --> Repo["SessionRepository"]
    Repo --> Decision{"active, not expired,\nclient/service match?"}
    Decision -->|yes| Valid["SESSION_VALID"]
    Decision -->|no| Invalid["SESSION_INVALID"]
    Valid --> App
    Invalid --> App
```

## Conceptual Messages

```mermaid
flowchart LR
    M1["Client request\nclientId, serviceId, requestId"] --> M2["AS request\nclient identity, TGS, timestamp"]
    M2 --> M3["AS response\nclient-TGS material, TGS ticket, lifetime\nsensitive data omitted"]
    M3 --> M4["TGS request\nTGS ticket, authenticator, serviceId, timestamp"]
    M4 --> M5["TGS response\nservice ticket, client-service material, lifetime\nsensitive data omitted"]
    M5 --> M6["Service request\nservice ticket, authenticator, timestamp"]
    M6 --> M7["Service response\naccess decision, protected output"]
    M7 --> M8["FLOW_RESULT\nsuccess/failure, opaque session, latency"]
    M8 --> M9["VERIFY_SESSION\nopaque sessionId, clientId, serviceId"]
```

## Negative Scenarios

```mermaid
flowchart TB
    Start["START_AUTH_FLOW"] --> ClientCheck{"known client?"}
    ClientCheck -->|no| UnknownClient["CLIENT_NOT_FOUND / UNKNOWN_CLIENT\nACCESS DENIED"]
    ClientCheck -->|yes| ServiceCheck{"known service?"}
    ServiceCheck -->|no| UnknownService["SERVICE_NOT_FOUND / TGS_UNKNOWN_SERVICE\nACCESS DENIED"]
    ServiceCheck -->|yes| ReplayCheck{"replay detected?"}
    ReplayCheck -->|yes| Replay["REPLAY rejected\nvalidated by tests"]
    ReplayCheck -->|no| Session["opaque session issued"]
    Session --> Verify{"VERIFY_SESSION valid?"}
    Verify -->|yes| Valid["SESSION_VALID"]
    Verify -->|no| Invalid["SESSION_INVALID"]
```

## WebSocket Contract

Entrada:

- `START_AUTH_FLOW`
- `VERIFY_SESSION`
- `LOGOUT_SESSION`
- `PING`

Salida:

- `FLOW_EVENT`
- `FLOW_RESULT`
- `SESSION_VALID`
- `SESSION_INVALID`
- `SESSION_LOGGED_OUT`
- `ERROR`
- `PONG`

`FLOW_RESULT.success=true` no es autorizacion final. La app debe esperar
`SESSION_VALID`.

## Security Notes

- No mostrar tickets crudos.
- No mostrar claves.
- No mostrar ciphertexts.
- No mostrar payloads sensibles.
- Enmascarar `sessionId`.
- Usar `wss://` para cloud.
