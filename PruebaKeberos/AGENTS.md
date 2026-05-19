# AGENTS.md

Instrucciones permanentes para futuros trabajos con Codex en este repositorio:

- No borrar la demo legacy en `Kerberos/` y `Seguridad/` sin autorizacion explicita y sin que pasen `mvn -q -DskipTests compile`, `mvn test` y la auditoria de dependencias legacy.
- No afirmar que el proyecto es MIT Kerberos oficial.
- No afirmar que el sistema esta listo para produccion critica.
- Mantener cambios pequenos, revisables y explicados.
- Correr `mvn test` cuando Maven este disponible; si falla o `mvn` no existe, documentar el resultado real.
- Si Maven no esta disponible, usar `javac` solo como verificacion complementaria de la demo local y dejar claro que no reemplaza `mvn test`.
- Mantener `README.md` y `docs/` sincronizados con los cambios tecnicos.
- No imprimir secretos, claves, tickets descifrados completos ni payloads sensibles en logs nuevos.
- Preferir DTOs tipados sobre `HashMap<String,Object>` en codigo nuevo.
- Mantener mappers legacy cuando sea necesario para no romper el runtime actual.
- La ruta modular nueva debe permanecer libre de `AESUtils`, `SealedObject` y Java serialization.
- La ruta modular es la ruta principal; la demo legacy solo debe tratarse como referencia historica mientras exista.
- Mantener `docs/audits/legacy-dependency-audit.md` y `docs/audits/legacy-removal-blockers.md` actualizados cuando se toque independencia legacy.
- Si se ejecuta auditoria modular, documentar o versionar la evidencia en `docs/audits/latest-run.md` y `docs/audits/latest-run.json`.
- Respetar `AUTH_MODE=demo/local` para demo y `AUTH_MODE=strict` para validacion sin secretos por defecto.
- Para cambios en runtime modular, cubrir al menos codec JSON, AES-GCM y flujo AS -> TGS -> Service cuando sea viable.
- Preferir documentacion honesta sobre afirmaciones exageradas.
- Mantener ejecucion local sin Docker como requisito actual.
- Dejar Docker y Docker Compose como trabajo futuro hasta que se autorice explicitamente.
- No introducir Spring Boot, frontend ni WebSockets salvo que la fase lo pida.
- Si se toca criptografia legacy, hacerlo con pruebas y sin romper AS -> TGS -> Service -> Client.
- Explicar siempre que cambio, como probarlo y que queda pendiente.
