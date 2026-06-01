export const SECURITY_VALIDATIONS = [
  {
    kind: "live",
    title: "Valid client and service",
    expected: "ACCESS GRANTED",
    flow: "START_AUTH_FLOW -> FLOW_RESULT -> VERIFY_SESSION -> SESSION_VALID",
    explanation: "A registered client requests a registered service and the app accepts access only after session verification.",
    evidence: "live/manual flow, validated by Maven and Docker evidence",
    type: "live/manual flow"
  },
  {
    kind: "negative",
    title: "Unknown client",
    expected: "ACCESS DENIED",
    flow: "START_AUTH_FLOW -> FLOW_RESULT failure",
    explanation: "The AS/Gateway path rejects identities that are not registered for the configured demo client.",
    evidence: "demo security scenario; automated tests cover CLIENT_NOT_FOUND/UNKNOWN_CLIENT",
    type: "demo security scenario",
    scenario: "unknown-client",
    buttonLabel: "Test Unknown Client"
  },
  {
    kind: "negative",
    title: "Unknown service",
    expected: "ACCESS DENIED",
    flow: "START_AUTH_FLOW -> TGS rejection -> FLOW_RESULT failure",
    explanation: "The TGS does not issue service access for a service ID that is not registered.",
    evidence: "demo security scenario; automated tests cover SERVICE_NOT_FOUND/TGS_UNKNOWN_SERVICE",
    type: "demo security scenario",
    scenario: "unknown-service",
    buttonLabel: "Test Unknown Service"
  },
  {
    kind: "negative",
    title: "Invalid session verification",
    expected: "SESSION_INVALID",
    flow: "VERIFY_SESSION with an unknown opaque sessionId",
    explanation: "An external app must keep access closed when the Gateway rejects the session.",
    evidence: "demo security scenario; Gateway tests cover invalid sessions",
    type: "demo security scenario",
    scenario: "invalid-session",
    buttonLabel: "Test Invalid Session"
  },
  {
    kind: "tested",
    title: "Replay protection",
    expected: "REPLAY rejected",
    flow: "Repeated authenticator/request identifiers are rejected by replay cache checks.",
    explanation: "Replay attack means reusing a previously valid request or authenticator. The replay cache accepts one use before expiration and rejects reuse.",
    evidence: "validated by Maven tests: InMemoryReplayCacheTest and modular flow replay tests",
    type: "validated by automated tests"
  },
  {
    kind: "tested",
    title: "Expired session",
    expected: "SESSION_INVALID",
    flow: "VERIFY_SESSION after expiration returns EXPIRED.",
    explanation: "Opaque sessions have an expiration and the Gateway rejects expired server-side state.",
    evidence: "validated by Maven tests: GatewaySessionServiceTest and repository tests",
    type: "validated by automated tests"
  },
  {
    kind: "tested",
    title: "Sensitive data exposure",
    expected: "tickets hidden, keys hidden, ciphertext hidden, sessionId masked",
    flow: "UI renders conceptual messages only and masks opaque session identifiers.",
    explanation: "The browser receives integration events and opaque session state, not raw protocol secrets.",
    evidence: "visual validation plus frontend sanitization",
    type: "validated by UI contract"
  },
  {
    kind: "evidence",
    title: "Infrastructure readiness",
    expected: "Terraform plan validated; no apply",
    flow: "terraform init -> terraform validate -> terraform plan",
    explanation: "AWS remains a deployment blueprint. No AWS resources were created for this validation.",
    evidence: "validated by Terraform plan evidence",
    type: "validated by Terraform plan"
  }
];
