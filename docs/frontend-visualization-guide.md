# Frontend Visualization Guide

La Fase 23A redisenia las dos UIs vanilla sin agregar frameworks.

## auth-web-demo

Nombre visual: Kerberos System Demonstration.

Pantalla principal: Kerberos Auth Control Center.

Muestra:

- estado de Gateway;
- storage mode si `/health` es accesible desde el browser;
- mode como no expuesto por el contrato WebSocket actual;
- latencia total del ultimo flujo;
- estado de sesion;
- mapa Client App -> WebSocket Gateway -> AS -> TGS -> Protected Service -> Opaque Session;
- mensajes conceptuales;
- Live Protocol Trace;
- Access Decision;
- Security Explanation;
- Security Validation Lab.

La UI no muestra secretos, claves, tickets completos, ciphertexts ni payloads
internos. `sessionId` se muestra enmascarado.

Archivos:

- `auth-web-demo/index.html`
- `auth-web-demo/src/styles.css`
- `auth-web-demo/src/app.js`
- `auth-web-demo/src/renderer.js`
- `auth-web-demo/src/state.js`
- `auth-web-demo/src/security-validations.js`

Validacion local:

```bash
cd auth-web-demo
node scripts/build.js
```

## sample-login-app

Nombre visual: MelodyFinder.

Muestra una app integradora realista:

- formulario con Gateway URL, clientId y serviceId;
- estado de Gateway;
- estado de autenticacion;
- estado de sesion;
- acceso concedido o denegado;
- panel "What is happening behind the scenes?";
- dashboard protegido solo despues de `SESSION_VALID`;
- logout con `LOGOUT_SESSION`.

Archivos:

- `sample-login-app/index.html`
- `sample-login-app/styles.css`
- `sample-login-app/app.js`
- `sample-login-app/assets/melodyfinder-mark.svg`

No usa npm ni frameworks frontend.
