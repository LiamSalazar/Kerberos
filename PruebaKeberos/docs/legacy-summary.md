# Legacy Summary

Antes de la ruta modular, el proyecto tuvo una implementacion historica de demo
con servidores y cliente escritos como clases Java directas, comunicacion por
serializacion de objetos y contratos basados en mapas.

Esa implementacion fue util para demostrar el flujo conceptual inicial:

- Authentication Server;
- Ticket Granting Server;
- Service Server;
- Client.

En Fase 8.1 fue reemplazada como ruta principal por los modulos `auth-*`, que
usan DTOs, JSON/TCP, AES-GCM con `CryptoEnvelope`, replay cache, configuracion
demo/strict, pruebas Maven y auditoria reproducible.

El codigo legacy fisico ya no forma parte del proyecto principal. Este documento
se conserva solo como referencia historica; no describe una ruta ejecutable
actual.
