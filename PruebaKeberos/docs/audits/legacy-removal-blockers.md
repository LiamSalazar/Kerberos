# Legacy Removal Blockers

Fecha: 2026-05-19

Estado final: bloqueadores resueltos en Fase 8.1.

## Bloqueadores Previos

- `mvn` no estaba disponible en el PATH de una sesion anterior.
- La eliminacion fisica estaba condicionada a compile, tests Maven y auditoria
  textual sin dependencias de paquetes historicos.

## Resolucion

- Maven fue ejecutado por ruta absoluta desde la instalacion embebida en
  NetBeans.
- `mvn -q -DskipTests compile` paso.
- `mvn test` paso con 52 tests, 0 failures, 0 errors, 0 skipped.
- La auditoria textual no encontro imports desde `auth-*` hacia los paquetes
  historicos.
- Las carpetas `Kerberos/`, `Seguridad/`, `Chat/` y `DistribucionClaves/`
  fueron retiradas.

## Seguimiento

Este archivo queda como registro historico. Los siguientes trabajos deben
enfocarse en retirar o renombrar adaptadores historicos internos de
`auth-transport` y variables `AUTH_LEGACY_*`, sin reintroducir codigo legacy
fisico.
