# auth-service

Aplicacion contenedora del servidor de servicio.

Aqui se ubicaran:

- validacion final de tickets de servicio
- entrega del recurso protegido
- adaptadores de servicio y configuracion local

Implementaciones actuales de `ProtectedResource`:

- `DemoProtectedResource`: respuesta local para demos.
- `HttpProtectedResource`: ejemplo de adaptador HTTP local simple para pruebas
  de integracion sin consumir APIs externas reales.
