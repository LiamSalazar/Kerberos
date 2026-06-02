# Non-Technical Explanation

This project is a technical model of distributed authentication. Imagine an app
that does not want to decide by itself whether someone can enter. Instead of
trusting a simple response, it asks a separate system.

The app talks to the WebSocket Gateway. The Gateway coordinates three internal
services:

1. AS confirms that the client exists.
2. TGS confirms that the requested service exists.
3. Service decides whether to answer the authenticated client.

When everything succeeds, the Gateway creates an opaque session. "Opaque" means
the browser only sees a partial identifier and cannot know or modify what it
represents. The app must ask the Gateway through `VERIFY_SESSION`. Only if it
receives `SESSION_VALID` does it open its protected area.

The `auth-web-demo` interface shows the technical map of the flow. MelodyFinder
in `sample-login-app` shows what a real app integrating the Gateway could look
like.

Secrets, keys, full tickets, and ciphertexts are not shown. The demo is local
and cloud-ready as a blueprint, but it does not claim to be ready for critical
production use.
