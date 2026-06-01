# Auth Web Demo

Demo frontend local para observar el flujo modular AS -> TGS -> Service a
traves de `auth-websocket-gateway`.

No usa React, Vite, TypeScript, bundler ni dependencias npm externas. Los
comandos `npm run dev` y `npm run build` usan scripts propios de Node.js.

Nombre visual de Fase 23A: **Kerberos System Demonstration**. La pantalla
principal es **Kerberos Auth Control Center**.

## Requisitos

- Node.js 18 o superior.
- npm.
- Backend modular levantado: AS, TGS, Service y WebSocket Gateway.

## Instalar

```bash
npm install
```

No instala paquetes externos; solo crea la metadata local de npm.

## Ejecutar

```bash
npm run dev
```

Default:

```text
http://127.0.0.1:5173
```

## Validar

```bash
npm run build
```

El build valida los archivos estaticos y copia la demo a `dist/`.

## Flujo Manual

1. Levantar `auth-as`, `auth-tgs` y `auth-service`.
2. Levantar `auth-websocket-gateway`.
3. Abrir la demo web local.
4. Mantener `ws://127.0.0.1:2800`.
5. Presionar `Connect`.
6. Presionar `Start Auth Flow`.
7. Verificar eventos `FLOW_*`, `FLOW_RESULT success`, `sessionId` enmascarado y
   `sessionExpiresAt`.
8. Usar `Verify Session` para enviar `VERIFY_SESSION`.
9. Usar `Logout Session` para enviar `LOGOUT_SESSION`.

## Visualizacion

La UI muestra:

- chips de conexion, storage/mode si estan disponibles, latencia y sesion;
- mapa Client App -> WebSocket Gateway -> AS -> TGS -> Service -> Opaque Session;
- mensajes conceptuales sin material sensible;
- Live Protocol Trace;
- Access Decision;
- Security Explanation;
- Security Validation Lab.

No muestra secretos, claves, tickets completos, ciphertexts ni material
criptografico sensible. `FLOW_RESULT.success=true` no debe usarse como
autorizacion final en una app real; se requiere `SESSION_VALID`.
