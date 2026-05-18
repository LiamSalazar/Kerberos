# AGENTS.md

Instrucciones permanentes para futuros trabajos con Codex en este repositorio:

- No borrar la demo legacy en `Kerberos/` y `Seguridad/` sin autorizacion explicita.
- No afirmar que el proyecto es MIT Kerberos oficial.
- No afirmar que el sistema esta listo para produccion critica.
- Mantener cambios pequenos, revisables y explicados.
- Correr `mvn test` cuando Maven este disponible; si falla o `mvn` no existe, documentar el resultado real.
- Si Maven no esta disponible, usar `javac` solo como verificacion complementaria de la demo local y dejar claro que no reemplaza `mvn test`.
- Mantener `README.md` y `docs/` sincronizados con los cambios tecnicos.
- No imprimir secretos, claves, tickets descifrados completos ni payloads sensibles en logs nuevos.
- Preferir DTOs tipados sobre `HashMap<String,Object>` en codigo nuevo.
- Mantener mappers legacy cuando sea necesario para no romper el runtime actual.
- Preferir documentacion honesta sobre afirmaciones exageradas.
- Mantener ejecucion local sin Docker como requisito actual.
- Dejar Docker y Docker Compose como trabajo futuro hasta que se autorice explicitamente.
- No introducir Spring Boot, frontend ni WebSockets salvo que la fase lo pida.
- Si se toca criptografia legacy, hacerlo con pruebas y sin romper AS -> TGS -> Service -> Client.
- Explicar siempre que cambio, como probarlo y que queda pendiente.
