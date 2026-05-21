# Protocol Flow

Este documento describe la ruta modular principal.

## Envelope

Todos los mensajes modulares viajan como JSON dentro de `ProtocolEnvelope`:

- `messageType`
- `version`
- `requestId`
- `issuedAt`
- `payload`

El transporte TCP usa un mensaje JSON por conexion. El servidor modular valida
payload no vacio, tamano maximo, timeout de lectura y, cuando se configura,
`MessageType` esperado por endpoint.

## Flujo

1. Client envia `AS_REQUEST` con `AsRequest`.
2. AS responde `AS_RESPONSE` con `CryptoEnvelope<SecureAsResponse>`.
3. Client envia `TGS_REQUEST` con `SecureTgsRequest`.
4. TGS valida replay, ticket, identidad, expiracion y clock skew.
5. TGS responde `TGS_RESPONSE` con `CryptoEnvelope<SecureTgsResponse>`.
6. Client envia `SERVICE_REQUEST` con `SecureServiceRequest`.
7. Service valida replay, ticket, identidad, expiracion y clock skew.
8. Service responde `SERVICE_RESPONSE` con `CryptoEnvelope<ServiceResponse>`.

## Gateway WebSocket

El gateway WebSocket es una capa externa al flujo principal. Recibe mensajes
JSON WebSocket y ejecuta el mismo flujo modular mediante `AuthClient`, por lo
que AS, TGS y Service siguen hablando JSON/TCP.

`auth-web-demo` consume este contrato desde el navegador local. El frontend solo
se comunica con el gateway; no abre conexiones directas con AS, TGS ni Service.

Entrada minima:

```json
{
  "type": "START_AUTH_FLOW",
  "requestId": "manual-1",
  "clientId": "1",
  "serviceId": "1"
}
```

Eventos emitidos:

- `FLOW_STARTED`
- `AS_REQUEST_SENT`
- `AS_RESPONSE_RECEIVED`
- `TGS_REQUEST_SENT`
- `TGS_RESPONSE_RECEIVED`
- `SERVICE_REQUEST_SENT`
- `SERVICE_RESPONSE_RECEIVED`
- `FLOW_SUCCESS`
- `FLOW_ERROR`

Resultado final:

```json
{
  "type": "FLOW_RESULT",
  "requestId": "manual-1",
  "success": true,
  "serviceMessage": "MODULAR AUTH EXITOSO",
  "asMillis": 1,
  "tgsMillis": 1,
  "serviceMillis": 1,
  "totalMillis": 3
}
```

## Errores Controlados

Los errores vuelven como `ERROR_RESPONSE` con payload `ErrorResponse`.

Casos cubiertos por pruebas unitarias, de componente o de integracion:

- flujo completo exitoso;
- replay y `requestId` repetido;
- servicio inexistente;
- cliente inexistente;
- ticket TGS expirado;
- ticket de servicio expirado;
- autenticador expirado o fuera de clock skew;
- ciphertext y `CryptoEnvelope` alterados;
- clave incorrecta;
- JSON vacio, truncado, malformado o con payload faltante;
- `MessageType` incorrecto;
- servidor no disponible;
- multiples clientes concurrentes;
- mensajes WebSocket validos, tipo desconocido, JSON invalido y servicios no
  disponibles a nivel de gateway.
- flujo WebSocket E2E real con AS, TGS, Service y Gateway levantados en pruebas
  Maven.

## Estado Legacy

La ruta de ejecucion historica fue retirada fisicamente del proyecto principal.
El resumen historico vive en `docs/legacy-summary.md`. La ruta modular no debe
presentarse como MIT Kerberos oficial ni como lista para produccion critica.
