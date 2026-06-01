# Non-Technical Explanation

Este proyecto es una maqueta tecnica de autenticacion distribuida. Imagine una
app que no quiere decidir sola si alguien puede entrar. En vez de confiar en una
respuesta simple, pregunta a un sistema separado.

La app habla con el WebSocket Gateway. El Gateway coordina tres servicios
internos:

1. AS confirma que el cliente existe.
2. TGS confirma que el servicio solicitado existe.
3. Service decide si responde al cliente autenticado.

Cuando todo sale bien, el Gateway crea una sesion opaca. "Opaca" significa que
el navegador solo ve un identificador parcial y no puede saber ni modificar lo
que representa. La app debe pedir al Gateway `VERIFY_SESSION`. Solo si recibe
`SESSION_VALID`, abre su area protegida.

La interfaz `auth-web-demo` muestra el mapa tecnico del flujo. MelodyFinder en
`sample-login-app` muestra como se veria una app real que integra el Gateway.

No se muestran secretos, claves, tickets completos ni ciphertexts. La demo es
local y cloud-ready como blueprint, pero no declara estar lista para produccion
critica.
