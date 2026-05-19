# AGENTS.md

Instrucciones permanentes para futuros trabajos con Codex en este repositorio:

- No afirmar que el proyecto es MIT Kerberos oficial.
- No afirmar que el sistema esta listo para produccion critica.
- La ruta principal es modular y vive en `auth-*`.
- No reintroducir las carpetas legacy retiradas sin autorizacion explicita.
- Mantener cambios pequenos, revisables y explicados.
- Correr `mvn test` cuando Maven este disponible; si falla o `mvn` no existe,
  documentar el resultado real.
- Si Maven no esta disponible en PATH, buscar una ruta absoluta disponible antes
  de declarar bloqueo.
- Mantener `README.md` y `docs/` sincronizados con los cambios tecnicos.
- No imprimir secretos, claves, tickets descifrados completos ni payloads
  sensibles en logs nuevos.
- Preferir DTOs tipados sobre `HashMap<String,Object>` en codigo nuevo.
- La ruta modular nueva debe permanecer libre de dependencias hacia paquetes
  historicos y Java serialization como contrato principal.
- Para cambios en runtime modular, cubrir al menos codec JSON, AES-GCM y flujo
  AS -> TGS -> Service cuando sea viable.
- Mantener `docs/audits/legacy-dependency-audit.md` actualizado cuando se toque
  independencia legacy.
- Si se ejecuta auditoria modular, documentar o versionar la evidencia en
  `docs/audits/latest-run.md` y `docs/audits/latest-run.json`.
- Respetar `AUTH_MODE=demo/local` para demo y `AUTH_MODE=strict` para
  validacion sin secretos por defecto.
- Preferir documentacion honesta sobre afirmaciones exageradas.
- Mantener ejecucion local sin Docker como requisito actual.
- Dejar Docker y Docker Compose como trabajo futuro hasta que se autorice
  explicitamente.
- No introducir Spring Boot, frontend ni WebSockets salvo que la fase lo pida.
- Explicar siempre que cambio, como probarlo y que queda pendiente.
