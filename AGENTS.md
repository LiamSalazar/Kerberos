# AGENTS.md

Instrucciones permanentes para futuros trabajos con Codex en este repositorio:

- No afirmar que el proyecto es MIT Kerberos oficial.
- No afirmar que el sistema esta listo para produccion critica.
- La ruta principal es modular y vive en `auth-*`.
- No reintroducir las carpetas legacy retiradas sin autorizacion explicita.
- Mantener cambios pequenos, revisables y explicados.
- Correr `mvn test` cuando se toque codigo o comportamiento; si falla,
  documentar el resultado real.
- Mantener `README.md` y `docs/` sincronizados con los cambios tecnicos.
- No imprimir secretos, claves, tickets descifrados completos ni payloads
  sensibles en logs nuevos.
- Preferir DTOs tipados sobre `HashMap<String,Object>` en codigo nuevo.
- La ruta modular nueva debe permanecer libre de dependencias hacia paquetes
  historicos y Java serialization como contrato principal.
- `auth-websocket-gateway` es una capa separada de integracion; no debe
  reemplazar ni acoplar WebSockets dentro de `auth-as`, `auth-tgs` o
  `auth-service`.
- `auth-web-demo` es una demo frontend local vanilla; mantenerla desacoplada del
  backend y comunicandose solo con `auth-websocket-gateway`.
- `sample-login-app` es una mini app vanilla para integradores; mantenerla
  desacoplada del backend y sin frameworks frontend salvo autorizacion futura.
- Las apps externas no deben conceder acceso solo por `FLOW_RESULT.success=true`;
  deben usar `VERIFY_SESSION` y aceptar acceso solo con `SESSION_VALID`.
- Mantener sesiones opacas del Gateway sin RSA, sin JWT, sin CA y sin exponer
  secretos, tickets, ciphertexts ni claves al frontend.
- Para cambios en runtime modular, cubrir al menos codec JSON, AES-GCM y flujo
  AS -> TGS -> Service cuando sea viable.
- Mantener `docs/audits/legacy-dependency-audit.md` actualizado cuando se toque
  independencia legacy.
- Si se ejecuta auditoria modular, documentar o versionar la evidencia en
  `docs/audits/latest-run.md` y `docs/audits/latest-run.json`.
- Si se ejecuta auditoria de concurrencia, documentar o versionar la evidencia
  en `docs/audits/concurrency-latest-run.md` y
  `docs/audits/concurrency-latest-run.json`.
- Mantener `docs/audits/maven-dependency-audit.md` actualizado cuando se toquen
  dependencias Maven/POM.
- Mantener `docs/audits/sqlite-audit-sample.md` y
  `docs/audits/sqlite-audit-sample.json` sincronizados cuando cambie el formato
  de auditoria SQLite.
- Respetar `AUTH_MODE=demo` para demo y `AUTH_MODE=strict` para
  validacion sin secretos por defecto.
- Mantener `AUTH_STORAGE_MODE=memory` como modo demo por defecto y
  `AUTH_STORAGE_MODE=sqlite` como integracion local verificable.
- No versionar bases generadas `*.db`, `*.sqlite` ni `*.sqlite3`.
- Preferir documentacion honesta sobre afirmaciones exageradas.
- Mantener ejecucion local sin Docker como requisito actual.
- Docker y Docker Compose estan autorizados solo como despliegue local
  reproducible; no presentarlos como produccion.
- No versionar herramientas portables locales como `tools/`.
- No introducir Spring Boot. No agregar frameworks frontend salvo que una fase
  futura lo autorice explicitamente.
- No agregar npm ni frameworks a `sample-login-app` sin autorizacion explicita.
- Explicar siempre que cambio, como probarlo y que queda pendiente.
