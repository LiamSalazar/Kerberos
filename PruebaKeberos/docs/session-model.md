# Opaque Session Model

Fase 18 cierra una brecha importante del Gateway: una app externa no debe
tratar `FLOW_RESULT.success=true` como autorizacion final. Ese campo es una
senal de que el flujo AS -> TGS -> Service termino bien, pero puede ser
manipulado si la app lo usa como estado local definitivo.

## Decision

El Gateway emite una sesion opaca, aleatoria y verificable del lado servidor.
No se usa RSA, JWT ni autoridad certificadora en esta fase.

La sesion opaca:

- no contiene secretos;
- no contiene tickets;
- no contiene ciphertexts;
- no contiene material criptografico sensible;
- vive en memoria o SQLite del sistema de autenticacion;
- se asocia a `requestId`, `clientId` y `serviceId`;
- expira;
- puede revocarse con `LOGOUT_SESSION`.

## Flow

1. La app envia `START_AUTH_FLOW`.
2. El Gateway ejecuta AS -> TGS -> Service.
3. Si el servicio concede acceso, el Gateway guarda una sesion server-side.
4. `FLOW_RESULT` devuelve `sessionId` y `sessionExpiresAt`.
5. La app envia `VERIFY_SESSION`.
6. Solo `SESSION_VALID` permite abrir el recurso propio.
7. En logout, la app envia `LOGOUT_SESSION`.

## Verification

`VERIFY_SESSION` requiere:

```json
{
  "type": "VERIFY_SESSION",
  "requestId": "verify-1",
  "sessionId": "opaque-session-id",
  "clientId": "app-client",
  "serviceId": "app-service"
}
```

El Gateway responde `SESSION_VALID` si la sesion existe, sigue activa, no
expiró y corresponde al mismo cliente y servicio. Si no, responde
`SESSION_INVALID` con una razon:

- `NOT_FOUND`
- `EXPIRED`
- `REVOKED`
- `CLIENT_MISMATCH`
- `SERVICE_MISMATCH`

## Expiration

La expiracion respeta el principio temporal del flujo: la sesion del Gateway no
debe durar mas que la respuesta del servicio. Cuando `ServiceResponse.expiresAt`
esta disponible, el Gateway usa el menor valor entre:

- `ServiceResponse.expiresAt`;
- `AUTH_SESSION_TTL_SECONDS`;
- `AUTH_SESSION_MAX_TTL_SECONDS`.

Si en una evolucion futura no pudiera propagarse `expiresAt`, el limite
operativo quedaria en `AUTH_SESSION_MAX_TTL_SECONDS` y debe documentarse.

## Storage

`AUTH_SESSION_STORAGE_MODE=memory|sqlite` controla donde vive la sesion. Si no
se configura, sigue el valor de `AUTH_STORAGE_MODE`; por eso `sqlite` persiste
sesiones en `auth_sessions` cuando el stack local usa SQLite.

SQLite sigue siendo interno al sistema de autenticacion. Una app externa no debe
abrir esa base para validar sesiones; debe usar `VERIFY_SESSION`.

## Limits

Este modelo no reemplaza TLS, WSS, politica de roles, permisos de negocio,
proteccion CSRF/cookies ni gestion productiva de secretos. `ws://` es solo para
desarrollo local; en cloud se debe usar `wss://` delante del Gateway.
