# Frontend Contract

Este contrato describe como `auth-web-demo`, `sample-login-app` y cualquier
futura interfaz web deben comunicarse con `auth-websocket-gateway`.

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
- `AUTH_ALLOWED_ORIGINS`: lista separada por comas para validar el header
  `Origin`. Si esta vacia, se permite ejecucion local sin restriccion de origen.

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

Campos requeridos:

- `type`
- `requestId`
- `clientId`
- `serviceId`

### PING

```json
{
  "type": "PING",
  "requestId": "ping-1"
}
```

Respuesta esperada: `PONG`.

## Mensajes De Salida

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

Cuando `success=false`, el mensaje incluye `errorType`:

```json
{
  "type": "FLOW_RESULT",
  "requestId": "front-req-2",
  "success": false,
  "errorType": "SERVICE_NOT_FOUND",
  "serviceMessage": "TGS_UNKNOWN_SERVICE: TGS o servicio no registrado"
}
```

### ERROR

Error de contrato, origen, rate limit o JSON invalido.

```json
{
  "type": "ERROR",
  "errorType": "MISSING_REQUIRED_FIELD",
  "message": "Campo requerido faltante: serviceId",
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

## Error Types

- `INVALID_JSON`
- `UNKNOWN_MESSAGE_TYPE`
- `MISSING_REQUIRED_FIELD`
- `CLIENT_NOT_FOUND`
- `SERVICE_NOT_FOUND`
- `FLOW_FAILED`
- `RATE_LIMITED`
- `ORIGIN_NOT_ALLOWED`

## Flujo Recomendado Para Frontend

1. Abrir `ws://127.0.0.1:2800`.
2. Enviar `START_AUTH_FLOW` con `requestId`, `clientId` y `serviceId`.
3. Renderizar cada `FLOW_EVENT` como progreso.
4. Al recibir `FLOW_RESULT`, cerrar o reutilizar la conexion segun la pantalla.
5. Conceder acceso solo si `FLOW_RESULT.success === true`.
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
latencias basicas. `auth-web-demo` evita renderizar detalles con nombres de
secretos, claves, tickets, ciphertexts, `CryptoEnvelope` o payloads internos. El
canal aun no tiene TLS ni autenticacion mutua; eso queda para una fase posterior.
Cuando el Gateway corre con `AUTH_STORAGE_MODE=sqlite`, registra eventos de alto
nivel en `auth_audit_events` sin incluir secretos ni payloads sensibles.
