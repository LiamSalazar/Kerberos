# WebSocket Gateway

`auth-websocket-gateway` expone una capa WebSocket separada para futuras
interfaces web. No reemplaza los servicios modulares principales: AS, TGS y
Service siguen ejecutandose como procesos TCP/JSON.

No incluye frontend, Docker ni Spring Boot.

## Dependencia

El modulo usa `org.java-websocket:Java-WebSocket`. Se eligio porque Java
estandar no incluye un servidor WebSocket simple para este caso y la alternativa
mantiene el alcance pequeno frente a frameworks completos.

No se agrego una dependencia JSON nueva. Los mensajes del gateway son planos y
el codec propio esta probado para JSON invalido, tipos desconocidos y campos
mal tipados.

## Flujo

1. Un cliente WebSocket conecta al gateway.
2. El gateway responde `GATEWAY_READY`.
3. El cliente envia `START_AUTH_FLOW`.
4. El gateway usa `AuthClient` para ejecutar AS -> TGS -> Service por TCP/JSON.
5. El gateway emite `FLOW_EVENT` por etapa.
6. El gateway responde `FLOW_RESULT` con estado final y latencias.

## Mensajes

Entrada:

- `START_AUTH_FLOW`
- `RUN_AUDIT_FLOW`
- `PING`

Salida:

- `GATEWAY_READY`
- `FLOW_EVENT`
- `FLOW_RESULT`
- `ERROR`
- `PONG`

Ejemplo:

```json
{"type":"START_AUTH_FLOW","requestId":"manual-1","clientId":"1","serviceId":"1"}
```

Eventos esperados:

- `FLOW_STARTED`
- `AS_REQUEST_SENT`
- `AS_RESPONSE_RECEIVED`
- `TGS_REQUEST_SENT`
- `TGS_RESPONSE_RECEIVED`
- `SERVICE_REQUEST_SENT`
- `SERVICE_RESPONSE_RECEIVED`
- `FLOW_SUCCESS`
- `FLOW_ERROR`

## Ejecucion Local

Levanta primero AS, TGS y Service:

```cmd
scripts\run-as.bat
scripts\run-tgs.bat
scripts\run-service.bat
```

Luego:

```cmd
scripts\run-websocket-gateway.bat
```

Linux/macOS:

```bash
scripts/run-as.sh
scripts/run-tgs.sh
scripts/run-service.sh
scripts/run-websocket-gateway.sh
```

Variables:

- `AUTH_WS_HOST`: default `127.0.0.1`.
- `AUTH_WS_PORT`: default `2800`.

## Prueba Manual

Con una herramienta externa como `websocat`:

```bash
websocat ws://127.0.0.1:2800
```

Enviar:

```json
{"type":"PING","requestId":"ping-1"}
```

o:

```json
{"type":"START_AUTH_FLOW","requestId":"manual-1","clientId":"1","serviceId":"1"}
```

## Pruebas Maven

```bash
mvn -pl auth-websocket-gateway -am test
```

La suite cubre serializacion de mensajes, tipos desconocidos, JSON invalido,
flujo exitoso con cliente falso y error controlado cuando los servicios no estan
disponibles.

## Limites

- No hay frontend en esta fase.
- El gateway no levanta AS/TGS/Service automaticamente.
- La prueba actual del gateway es principalmente unitaria/componente; una
  prueba end-to-end con un cliente WebSocket real queda como mejora futura.
- No hay TLS ni autenticacion mutua en el canal WebSocket.
