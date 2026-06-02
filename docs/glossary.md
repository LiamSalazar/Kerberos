# Glossary

| Term | Meaning |
| --- | --- |
| AS | Authentication Server. Validates the client's initial identity. |
| TGS | Ticket Granting Server. Provides conceptual access to the requested service. |
| Service | Protected service that returns or denies the resource. |
| Gateway | WebSocket layer for external apps. |
| AuthClient | Internal client that runs AS -> TGS -> Service. |
| Ticket | Temporary proof for another server; it is not exposed raw to the browser. |
| Authenticator | Temporary message used to prove freshness. |
| Timestamp | Time mark used for expiration and replay protection. |
| Replay attack | Reusing a previously valid message. |
| Replay cache | Temporary registry that rejects reuse. |
| AES | Symmetric encryption used by the modular path. |
| AES-GCM | Authenticated AES with integrity protection. |
| Session | Temporary access state. |
| Opaque session | Session stored server-side and represented by an opaque ID. |
| `FLOW_RESULT` | Authentication flow result. |
| `VERIFY_SESSION` | Message that validates the opaque session. |
| `SESSION_VALID` | Response that allows access to be granted. |
| `SESSION_INVALID` | Response that requires access to be denied. |
| Docker | Local container runtime. |
| Docker Compose | Local container orchestration. |
| SQLite | Lightweight local database for demo/verification. |
| PostgreSQL | Database prepared for shared sessions and RDS. |
| RDS | Managed PostgreSQL on AWS. |
| Terraform | Infrastructure as code. |
| ECS/Fargate | Cloud container execution. |
| ECR | Docker image registry. |
| ALB | Public load balancer. |
| ACM | Managed TLS certificates. |
| WSS | Secure WebSocket over TLS. |
| Secrets Manager | Recommended cloud service for secrets. |
| CloudWatch | Logs and metrics on AWS. |
| Service Discovery | Internal service resolution. |
| Public subnet | Subnet suitable for a public ALB. |
| Private subnet | Subnet for internal services and RDS. |
