# Concurrency

Fase 14 agrega pruebas y auditoria de concurrencia para la ruta modular
AS -> TGS -> Service. El objetivo es verificar que multiples flujos simultaneos
puedan completarse sin colisiones de `requestId` ni falsos positivos de replay.

## Prueba Automatizada

La suite Maven incluye `ModularAuthConcurrencyTest` en `auth-client-sdk`.

La prueba levanta AS, TGS y Service modulares en puertos dinamicos y ejecuta:

- 10 clientes concurrentes;
- 50 flujos totales;
- `requestId` y autenticadores unicos por flujo;
- validacion de exitos, fallos, latencia promedio, p95 y throughput.

Ejecutar:

```bash
mvn -pl auth-client-sdk -am test
```

Tambien queda cubierta por:

```bash
mvn test
```

## Auditoria Reproducible

Con AS, TGS y Service levantados:

```cmd
scripts\run-concurrency-audit.bat --clients 25 --flows 100
```

Linux/macOS:

```bash
scripts/run-concurrency-audit.sh --clients 25 --flows 100
```

El runner `ModularAuthConcurrencyAuditRunner` genera:

- `docs/audits/concurrency-latest-run.md`
- `docs/audits/concurrency-latest-run.json`

El reporte registra fecha/hora, Java, sistema operativo, commit, comando,
clientes concurrentes, flujos totales, exitos, fallos, latencia minima,
latencia maxima, promedio, p95, throughput aproximado y errores por tipo.

No registra secretos, claves, tickets completos, ciphertexts ni material
criptografico sensible.

La auditoria persistente SQLite de Fase 15 es independiente de esta auditoria de
concurrencia. El Gateway registra eventos por flujo en `auth_audit_events`
cuando se ejecuta con `AUTH_STORAGE_MODE=sqlite`; el runner de concurrencia
mantiene su evidencia en Markdown/JSON bajo `docs/audits/`.

## Ultima Evidencia Versionada

La ultima corrida versionada fue:

- clientes concurrentes: 25;
- flujos totales: 100;
- exitos: 100;
- fallos: 0;
- throughput aproximado: 9.039 flujos/s;
- p95 total: 5698.616 ms.

Ver `docs/audits/concurrency-latest-run.md` para el detalle.

## Limites

- La prueba usa servidores locales en memoria o el modo configurado por test.
- No mide comportamiento distribuido entre multiples maquinas.
- El replay cache sigue siendo local por proceso.
- No sustituye pruebas de carga reales ni observabilidad productiva.
