# Glossary

| Termino | Significado |
| --- | --- |
| AS | Authentication Server. Valida identidad inicial del cliente. |
| TGS | Ticket Granting Server. Entrega acceso conceptual al servicio solicitado. |
| Service | Servicio protegido que devuelve o niega el recurso. |
| Gateway | Capa WebSocket para apps externas. |
| AuthClient | Cliente interno que ejecuta AS -> TGS -> Service. |
| Ticket | Prueba temporal para otro servidor; no se expone cruda al browser. |
| Authenticator | Mensaje temporal usado para demostrar frescura. |
| Timestamp | Marca temporal usada para vencimiento y replay protection. |
| Replay attack | Reutilizar un mensaje valido anterior. |
| Replay cache | Registro temporal que rechaza reutilizaciones. |
| AES | Cifrado simetrico usado por la ruta modular. |
| AES-GCM | AES autenticado con proteccion de integridad. |
| Session | Estado temporal de acceso. |
| Opaque session | Sesion guardada server-side y representada por un ID opaco. |
| `FLOW_RESULT` | Resultado del flujo de autenticacion. |
| `VERIFY_SESSION` | Mensaje que valida la sesion opaca. |
| `SESSION_VALID` | Respuesta que permite conceder acceso. |
| `SESSION_INVALID` | Respuesta que exige negar acceso. |
| Docker | Runtime local de contenedores. |
| Docker Compose | Orquestacion local de contenedores. |
| SQLite | Base local ligera para demo/verificacion. |
| PostgreSQL | Base preparada para sesiones compartidas y RDS. |
| RDS | PostgreSQL administrado en AWS. |
| Terraform | Infraestructura como codigo. |
| ECS/Fargate | Ejecucion cloud de contenedores. |
| ECR | Registry de imagenes Docker. |
| ALB | Balanceador de carga publico. |
| ACM | Certificados TLS administrados. |
| WSS | WebSocket seguro sobre TLS. |
| Secrets Manager | Servicio cloud recomendado para secretos. |
| CloudWatch | Logs y metricas en AWS. |
| Service Discovery | Resolucion interna entre servicios. |
| Public subnet | Subnet apta para ALB publico. |
| Private subnet | Subnet para servicios internos y RDS. |
