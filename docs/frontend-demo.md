# Frontend Demo

`auth-web-demo` es la demo tecnica local para observar el flujo modular
AS -> TGS -> Service a traves de `auth-websocket-gateway`.

No es la app de login. Para el ejemplo de integracion con zona protegida, usar
`sample-login-app`.

## What It Shows

- header `Kerberos Auth Control Center`;
- chips de Gateway, storage/mode si estan disponibles, latencia y sesion;
- panel de conexion con `START_AUTH_FLOW`, `VERIFY_SESSION`, `LOGOUT_SESSION`
  y `Clear Trace`;
- mapa Client App -> WebSocket Gateway -> AS -> TGS -> Service -> Opaque Session;
- tarjetas de mensajes conceptuales;
- Live Protocol Trace;
- Access Decision;
- Security Explanation;
- Security Validation Lab;
- errores de protocolo sin mostrar secretos.

## Run

Windows:

```cmd
scripts\run-as.bat
scripts\run-tgs.bat
scripts\run-service.bat
scripts\run-websocket-gateway.bat
scripts\run-web-demo.bat
```

Linux/macOS:

```bash
scripts/run-as.sh
scripts/run-tgs.sh
scripts/run-service.sh
scripts/run-websocket-gateway.sh
scripts/run-web-demo.sh
```

Abrir:

```text
http://127.0.0.1:5173
```

## Build Validation

```bash
cd auth-web-demo
npm install
npm run build
```

`auth-web-demo/dist/` es generado y no debe versionarse.

## Security Display

La UI muestra solo estado, etapas, mensajes de alto nivel, latencias y sesion
opaca enmascarada. No renderiza secretos, tickets completos, claves,
ciphertexts, `CryptoEnvelope` ni payloads internos.

`FLOW_RESULT.success=true` no debe interpretarse como autorizacion final en una
app real. La autorizacion practica requiere `VERIFY_SESSION` y `SESSION_VALID`.
