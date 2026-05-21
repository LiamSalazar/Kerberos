# Frontend Contract

Este contrato describe como una futura interfaz web debe comunicarse con
`auth-websocket-gateway`. No hay frontend implementado en esta fase.

El gateway es una capa de integracion: no reemplaza `auth-as`, `auth-tgs` ni
`auth-service`, y no expone tickets, claves ni ciphertexts al cliente.

## URL Local

Default:

```text
ws://127.0.0.1:2800
```

Variables:

- `AUTH_WS_HOST`: host del gateway. Default `127.0.0.1`.
- `AUTH_WS_PORT`: puerto del gateway. Default `2800`.

## Mensajes De Entrada

### START_AUTH_FLOW

Ejecuta AS -> TGS -> Service mediante la ruta TCP modular.

```json
{
  "type": "START_AUTH_FLOW",
  "requestId": "front-req-1",
  "clientId": "1",
  "serviceId": "1"
}
```

Campos:

- `type`: requerido.
- `requestId`: recomendado. Si llega vacio o `null`, el gateway genera uno.
- `clientId`: recomendado. Actualmente debe coincidir con el cliente configurado.
- `serviceId`: recomendado. Default `1` si se omite.

### RUN_AUDIT_FLOW

Usa el mismo contrato de `START_AUTH_FLOW` para ejecutar un flujo observable.
No reemplaza el audit runner de consola.

### PING

```json
{
  "type": "PING",
  "requestId": "ping-1"
}
```

Respuesta esperada: `PONG`.

## Mensajes De Salida

### GATEWAY_READY

Se envia al abrir la conexion.

```json
{
  "type": "GATEWAY_READY",
  "message": "WebSocket Gateway listo"
}
```

### FLOW_EVENT

Evento de progreso.

```json
{
  "type": "FLOW_EVENT",
  "requestId": "front-req-1",
  "stage": "AS_RESPONSE_RECEIVED",
  "message": "TGT emitido"
}
```

Stages actuales:

- `FLOW_STARTED`
- `AS_REQUEST_SENT`
- `AS_RESPONSE_RECEIVED`
- `TGS_REQUEST_SENT`
- `TGS_RESPONSE_RECEIVED`
- `SERVICE_REQUEST_SENT`
- `SERVICE_RESPONSE_RECEIVED`
- `FLOW_SUCCESS`
- `FLOW_ERROR`

### FLOW_RESULT

Resultado terminal del flujo.

```json
{
  "type": "FLOW_RESULT",
  "requestId": "front-req-1",
  "success": true,
  "serviceMessage": "--------- ACCESO CONCEDIDO A MELODYFINDER --------- MODULAR AUTH EXITOSO ---------",
  "asMillis": 4,
  "tgsMillis": 3,
  "serviceMillis": 3,
  "totalMillis": 12
}
```

### ERROR

Error de contrato o mensaje invalido.

```json
{
  "type": "ERROR",
  "message": "WebSocketMessageType no soportado: UNKNOWN",
  "success": false
}
```

### PONG

```json
{
  "type": "PONG",
  "requestId": "ping-1",
  "message": "pong"
}
```

## Errores Esperados

- JSON invalido: `ERROR`.
- `type` faltante: `ERROR`.
- tipo desconocido: `ERROR`.
- `clientId` inexistente: `FLOW_EVENT` con `FLOW_ERROR` y `FLOW_RESULT` con
  `success=false`.
- `serviceId` inexistente: `FLOW_EVENT` con `FLOW_ERROR` y `FLOW_RESULT` con
  `success=false`.
- AS/TGS/Service no disponibles: `FLOW_EVENT` con `FLOW_ERROR` y `FLOW_RESULT`
  con `success=false`.

## Flujo Recomendado Para Frontend

1. Abrir `ws://127.0.0.1:2800`.
2. Esperar `GATEWAY_READY`.
3. Enviar `START_AUTH_FLOW` con `requestId` generado por el frontend.
4. Renderizar cada `FLOW_EVENT` como progreso.
5. Al recibir `FLOW_RESULT`, cerrar o reutilizar la conexion segun la pantalla.
6. Mostrar `ERROR` como fallo de contrato del gateway.

## Seguridad

El frontend no debe recibir ni pedir:

- secretos demo;
- claves de sesion;
- tickets completos;
- `CryptoEnvelope` completo;
- ciphertexts;
- payloads internos AS/TGS/Service.

Los mensajes WebSocket actuales exponen solo estado, texto de alto nivel y
latencias basicas. El canal aun no tiene TLS ni autenticacion mutua; eso queda
para una fase posterior.
