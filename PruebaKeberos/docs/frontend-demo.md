# Frontend Demo

`auth-web-demo` es una demo web local para visualizar el flujo modular
AS -> TGS -> Service a traves de `auth-websocket-gateway`.

No usa React, Vite, TypeScript, bundler ni dependencias npm externas. Es HTML,
CSS y JavaScript vanilla con scripts Node propios para servir y validar archivos
estaticos.

## Requisitos

- Java 17+ y Maven para el backend.
- Node.js 18+ y npm para la demo web.
- Cinco terminales locales.
- Docker no es requisito y queda para una fase futura.

## Ejecutar Backend Y Gateway

Windows:

```cmd
scripts\run-as.bat
scripts\run-tgs.bat
scripts\run-service.bat
scripts\run-websocket-gateway.bat
```

Linux/macOS:

```bash
scripts/run-as.sh
scripts/run-tgs.sh
scripts/run-service.sh
scripts/run-websocket-gateway.sh
```

El gateway escucha por defecto en:

```text
ws://127.0.0.1:2800
```

## Ejecutar Frontend

Primera vez:

```bash
cd auth-web-demo
npm install
npm run build
```

Ejecutar desde la raiz del proyecto:

```cmd
scripts\run-web-demo.bat
```

Linux/macOS:

```bash
scripts/run-web-demo.sh
```

Default:

```text
http://127.0.0.1:5173
```

## Flujo Manual

1. Levanta AS, TGS y Service.
2. Levanta `auth-websocket-gateway`.
3. Levanta `auth-web-demo`.
4. Abre `http://127.0.0.1:5173`.
5. Usa `ws://127.0.0.1:2800`.
6. Pulsa `Connect`.
7. Pulsa `Start Auth Flow`.
8. Verifica eventos `FLOW_*`.
9. Verifica `FLOW_RESULT success=true`.
10. Verifica que el servicio concedido sea visible en `Final Result`.

## Eventos Esperados

- `GATEWAY_READY`
- `FLOW_STARTED`
- `AS_REQUEST_SENT`
- `AS_RESPONSE_RECEIVED`
- `TGS_REQUEST_SENT`
- `TGS_RESPONSE_RECEIVED`
- `SERVICE_REQUEST_SENT`
- `SERVICE_RESPONSE_RECEIVED`
- `FLOW_SUCCESS`
- `FLOW_RESULT`

## Errores Comunes

- Gateway no disponible: la UI muestra error de conexion.
- WebSocket cerrado durante el flujo: la UI marca el flujo como error.
- JSON invalido desde el servidor: la UI registra error de protocolo.
- Respuesta `ERROR`: la UI la muestra en el panel de errores.
- Timeout sin `FLOW_RESULT`: la UI marca timeout de flujo.

## Seguridad De Visualizacion

La UI muestra solo etapa, estado, mensaje resumido, `requestId`, latencias y
resultado final del servicio. No renderiza secretos, claves, tickets completos,
ciphertexts, `CryptoEnvelope` ni payloads internos.

## Validacion

```bash
cd auth-web-demo
npm install
npm run build
```

El build valida que existan los archivos estaticos principales y copia la demo a
`auth-web-demo/dist/`, carpeta generada que no se versiona.
