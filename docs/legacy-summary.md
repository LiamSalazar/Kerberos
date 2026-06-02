# Legacy Summary

Before the modular path, the project had a historical demo implementation with
servers and client written as direct Java classes, object-serialization
communication, and map-based contracts.

That implementation was useful to demonstrate the initial conceptual flow:

- Authentication Server;
- Ticket Granting Server;
- Service Server;
- Client.

In Phase 8.1 it was replaced as the main path by the `auth-*` modules, which
use DTOs, JSON/TCP, AES-GCM with `CryptoEnvelope`, replay cache,
demo/strict configuration, Maven tests, and reproducible audit evidence.

In Phase 9, the internal transport adapters that preserved Java serialization
and historical mappers were also removed.

The physical legacy code is no longer part of the main project. This document
is kept only as historical reference; it does not describe a currently
executable path.
