# Legacy Removal Blockers

Fecha: 2026-05-19

Estado final de esta fase: legacy no eliminado.

## Bloqueadores

- `mvn -q -DskipTests compile` no pudo ejecutarse porque `mvn` no esta en el
  PATH del entorno actual.
- `mvn test` no pudo ejecutarse por la misma razon.
- La regla de eliminacion controlada exige que ambos comandos Maven pasen antes
  de borrar `Kerberos/` y `Seguridad/`.

## Evidencia Disponible

- La auditoria textual no encontro imports desde `auth-*` hacia `Kerberos.*` o
  `Seguridad.*`.
- La ruta modular compilo con `javac` como verificacion auxiliar.
- El audit runner modular ejecuto 3 iteraciones AS -> TGS -> Service en puertos
  `2500`, `2501` y `2502` con 3 exitos y 0 fallos.
- La evidencia de ejecucion quedo en:
  - `docs/audits/latest-run.md`
  - `docs/audits/latest-run.json`

## Tareas Para Eliminar Legacy Despues

1. Instalar Maven o agregarlo al PATH.
2. Ejecutar `mvn -q -DskipTests compile`.
3. Ejecutar `mvn test`.
4. Confirmar nuevamente que no hay imports desde `auth-*` hacia `Kerberos` o
   `Seguridad`.
5. Borrar `Kerberos/` y `Seguridad/` solo si los gates anteriores pasan.
6. Retirar o mover a documentacion historica los adaptadores
   `auth-transport/legacy` y `auth-transport/javaio` si ya no aportan valor.
