# Frontend Visualization Guide

Phase 23A redesigns both vanilla UIs without adding frameworks.

## auth-web-demo

Visual name: Kerberos System Demonstration.

Main screen: Kerberos Auth Control Center.

Shows:

- Gateway status;
- storage mode if `/health` is accessible from the browser;
- mode as not exposed by the current WebSocket contract;
- total latency of the latest flow;
- session status;
- map Client App -> WebSocket Gateway -> AS -> TGS -> Protected Service -> Opaque Session;
- conceptual messages;
- Live Protocol Trace;
- Access Decision;
- Security Explanation;
- Security Validation Lab.

The UI does not show secrets, keys, full tickets, ciphertexts, or internal
payloads. `sessionId` is shown masked.

Files:

- `auth-web-demo/index.html`
- `auth-web-demo/src/styles.css`
- `auth-web-demo/src/app.js`
- `auth-web-demo/src/renderer.js`
- `auth-web-demo/src/state.js`
- `auth-web-demo/src/security-validations.js`

Local validation:

```bash
cd auth-web-demo
node scripts/build.js
```

## sample-login-app

Visual name: MelodyFinder.

Shows a realistic integrator app:

- form with Gateway URL, clientId, and serviceId;
- Gateway status;
- authentication status;
- session status;
- access granted or denied;
- panel "What is happening behind the scenes?";
- protected dashboard only after `SESSION_VALID`;
- logout with `LOGOUT_SESSION`.

Files:

- `sample-login-app/index.html`
- `sample-login-app/styles.css`
- `sample-login-app/app.js`
- `sample-login-app/assets/melodyfinder-mark.svg`

It does not use npm or frontend frameworks.
