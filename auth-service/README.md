# auth-service

Container application for the service server.

This module contains:

- final validation of service tickets
- protected resource delivery
- service adapters and local configuration

Current `ProtectedResource` implementations:

- `DemoProtectedResource`: local response for demos.
- `HttpProtectedResource`: simple local HTTP adapter example for integration
  tests without calling real external APIs.
