import {
  applyFlowStage,
  applyMessageStatus,
  applyResultLatencies,
  applySessionStatus,
  createInitialState,
  createProtocolState,
  createRequestId,
  createWaitingResult,
  latencyParts,
  stageDescription
} from "./state.js";
import { createRenderer } from "./renderer.js";
import { GatewayWebSocketClient, assertWebSocketSupport } from "./websocket-client.js";
import { SECURITY_VALIDATIONS } from "./security-validations.js";

const FLOW_TIMEOUT_MS = 15_000;

const state = createInitialState(SECURITY_VALIDATIONS);
let flowTimeoutId = null;

const gateway = new GatewayWebSocketClient({
  onOpen() {
    state.connection.status = "connected";
    state.connection.label = "Gateway connected";
    addEvent({ type: "LOCAL", message: "WebSocket connected.", description: "Browser is connected only to the Gateway." });
    refreshGatewayHealth();
    render();
  },
  onClose() {
    state.connection.status = "closed";
    state.connection.label = "Disconnected";
    if (state.flowRunning) {
      finishFlowWithError("WebSocket closed before FLOW_RESULT.");
    }
    render();
  },
  onError(message) {
    state.connection.status = "error";
    state.connection.label = "Connection error";
    addError(message);
    render();
  },
  onProtocolError(message) {
    addError(message);
    render();
  },
  onMessage(message) {
    handleGatewayMessage(message);
  }
});

const renderer = createRenderer({
  connect(inputs) {
    updateInputs(inputs);
    connectGateway();
  },
  startFlow(inputs) {
    updateInputs(inputs);
    startAuthFlow();
  },
  verifySession(inputs) {
    updateInputs(inputs);
    verifySession();
  },
  logoutSession() {
    logoutSession();
  },
  clearEvents() {
    clearTrace();
  },
  runScenario(kind, inputs) {
    updateInputs(inputs);
    runSecurityScenario(kind);
  }
});

init();

function init() {
  try {
    assertWebSocketSupport();
    render();
  } catch (error) {
    addError(error.message);
    render();
  }
}

function connectGateway() {
  if (!state.inputs.wsUrl) {
    addError("WebSocket URL is required.");
    render();
    return;
  }

  state.connection.status = "connecting";
  state.connection.label = "Connecting";
  state.connection.url = state.inputs.wsUrl;
  state.environment.healthUrl = inferHealthUrl(state.inputs.wsUrl);
  render();

  try {
    gateway.connect(state.inputs.wsUrl);
  } catch (error) {
    state.connection.status = "error";
    state.connection.label = "Connection error";
    addError(error.message);
    render();
  }
}

function startAuthFlow(overrides = {}) {
  if (!gateway.isConnected()) {
    addError("Gateway unavailable. Connect the WebSocket before starting the flow.");
    render();
    return;
  }

  const requestId = createRequestId(overrides.prefix || "web");
  const clientId = overrides.clientId || state.inputs.clientId || "1";
  const serviceId = overrides.serviceId || state.inputs.serviceId || "1";

  resetLiveFlow(requestId);
  state.result = {
    ...createWaitingResult(),
    decision: "AUTHENTICATING",
    status: "running",
    requestId,
    serviceMessage: "Waiting for FLOW_RESULT.",
    sessionState: "not issued"
  };
  state.protocol = applyMessageStatus(state.protocol, "start_auth_flow", "active");
  state.protocol.nodes.client.status = "processing";
  state.protocol.nodes.client.event = "START_AUTH_FLOW";
  state.protocol.nodes.gateway.status = "waiting";
  state.protocol.links["client-gateway"] = "active";
  addEvent({
    type: "START_AUTH_FLOW",
    stage: "CLIENT_REQUEST",
    requestId,
    message: "START_AUTH_FLOW sent to Gateway.",
    description: "Contains clientId, serviceId and requestId only."
  });
  render();

  try {
    gateway.send({
      type: "START_AUTH_FLOW",
      requestId,
      clientId,
      serviceId
    });
    armFlowTimeout(requestId);
  } catch (error) {
    finishFlowWithError(error.message);
  }
}

function handleGatewayMessage(message) {
  switch (message.type) {
    case "FLOW_EVENT":
      handleFlowEvent(message);
      break;
    case "FLOW_RESULT":
      handleFlowResult(message);
      break;
    case "ERROR":
      handleGatewayError(message);
      break;
    case "SESSION_VALID":
      handleSessionValid(message);
      break;
    case "SESSION_INVALID":
      handleSessionInvalid(message);
      break;
    case "SESSION_LOGGED_OUT":
      handleSessionLoggedOut(message);
      break;
    case "PONG":
      addEvent({
        type: "PONG",
        requestId: message.requestId,
        message: message.message || "pong",
        description: "Gateway heartbeat response."
      });
      break;
    default:
      addError(`Unknown WebSocket message type: ${message.type || "none"}`);
      break;
  }
  render();
}

function handleFlowEvent(message) {
  const stage = message.stage || "FLOW_EVENT";
  state.activeStage = stage;
  state.protocol = applyFlowStage(state.protocol, stage, message.message);
  addEvent({
    type: message.type,
    stage,
    requestId: message.requestId,
    message: message.message || stage,
    description: stageDescription(stage)
  });

  if (stage === "FLOW_ERROR") {
    state.flowRunning = false;
    state.result = {
      ...state.result,
      decision: "ACCESS DENIED",
      status: "error",
      success: false,
      serviceMessage: message.message || "Controlled flow error."
    };
  }
}

function handleFlowResult(message) {
  clearFlowTimeout();
  state.flowRunning = false;
  state.activeStage = "FLOW_RESULT";
  const latencies = latencyParts(message);
  state.protocol = applyResultLatencies(state.protocol, latencies);
  state.protocol = applyMessageStatus(
    state.protocol,
    "flow_result",
    message.success ? "success" : "error"
  );

  if (message.success) {
    state.protocol = applySessionStatus(state.protocol, "waiting", "Session issued; verification pending");
  } else {
    state.protocol = applyFlowStage(state.protocol, "FLOW_ERROR", message.serviceMessage || message.errorType);
  }

  state.result = {
    decision: message.success ? "ACCESS GRANTED" : "ACCESS DENIED",
    status: message.success ? "success" : "error",
    requestId: message.requestId || state.activeRequestId || "none",
    serviceMessage: message.serviceMessage || "No service message returned.",
    sessionId: message.sessionId || "none",
    sessionExpiresAt: message.sessionExpiresAt || "not available",
    sessionState: message.sessionId ? "issued; verification required" : "not issued",
    success: Boolean(message.success),
    errorType: message.errorType || null,
    latencies
  };

  addEvent({
    type: "FLOW_RESULT",
    stage: message.success ? "FLOW_SUCCESS" : "FLOW_ERROR",
    requestId: state.result.requestId,
    message: message.success ? "Opaque session issued by Gateway." : (message.errorType || "Flow failed."),
    description: message.success
      ? "The app still must send VERIFY_SESSION before accepting access."
      : "Access remains closed; no opaque session was issued."
  });

  if (!message.success) {
    addError(message.serviceMessage || message.errorType || "The flow ended with a controlled error.");
  }
}

function verifySession() {
  if (!gateway.isConnected()) {
    addError("Gateway unavailable. Connect the WebSocket before verifying the session.");
    render();
    return;
  }
  if (!state.result.sessionId || state.result.sessionId === "none") {
    addError("No issued sessionId is available for verification.");
    render();
    return;
  }
  sendVerifySession(state.result.sessionId, `${state.result.requestId}-verify`);
}

function logoutSession() {
  if (!gateway.isConnected()) {
    addError("Gateway unavailable. Connect the WebSocket before logout.");
    render();
    return;
  }
  if (!state.result.sessionId || state.result.sessionId === "none") {
    addError("No issued sessionId is available for logout.");
    render();
    return;
  }
  const requestId = `${state.result.requestId}-logout`;
  gateway.send({
    type: "LOGOUT_SESSION",
    requestId,
    sessionId: state.result.sessionId
  });
  state.result = {
    ...state.result,
    decision: "SESSION REVOKED",
    sessionState: "logging out"
  };
  state.protocol = applySessionStatus(state.protocol, "processing", "LOGOUT_SESSION");
  addEvent({
    type: "LOGOUT_SESSION",
    stage: "SESSION_LOGOUT",
    requestId,
    message: "LOGOUT_SESSION sent to Gateway.",
    description: "The opaque session is revoked server-side."
  });
  render();
}

function sendVerifySession(sessionId, requestId) {
  gateway.send({
    type: "VERIFY_SESSION",
    requestId,
    sessionId,
    clientId: state.inputs.clientId || "1",
    serviceId: state.inputs.serviceId || "1"
  });
  state.result = {
    ...state.result,
    decision: "AUTHENTICATING",
    sessionState: "verifying"
  };
  state.protocol = applyMessageStatus(state.protocol, "verify_session", "active");
  state.protocol = applySessionStatus(state.protocol, "processing", "VERIFY_SESSION");
  addEvent({
    type: "VERIFY_SESSION",
    stage: "SESSION_VERIFY",
    requestId,
    message: "VERIFY_SESSION sent to Gateway.",
    description: "Access should be accepted only after SESSION_VALID."
  });
}

function handleSessionValid(message) {
  state.result = {
    ...state.result,
    decision: "SESSION VALID",
    status: "verified",
    sessionState: "valid",
    sessionExpiresAt: message.expiresAt || state.result.sessionExpiresAt,
    sessionId: message.sessionId || state.result.sessionId
  };
  state.protocol = applyMessageStatus(state.protocol, "verify_session", "success");
  state.protocol = applyMessageStatus(state.protocol, "session_valid", "success");
  state.protocol = applySessionStatus(state.protocol, "success", "SESSION_VALID");
  addEvent({
    type: "SESSION_VALID",
    stage: "SESSION_VERIFY",
    requestId: message.requestId,
    message: "SESSION_VALID received.",
    description: "The Gateway confirms the opaque session is active for this client and service."
  });
}

function handleSessionInvalid(message) {
  state.result = {
    ...state.result,
    decision: "ACCESS DENIED",
    status: "error",
    sessionState: `invalid: ${message.reason || "unknown"}`,
    success: false
  };
  state.protocol = applyMessageStatus(state.protocol, "verify_session", "error");
  state.protocol = applySessionStatus(state.protocol, "error", "SESSION_INVALID");
  addEvent({
    type: "SESSION_INVALID",
    stage: "SESSION_VERIFY",
    requestId: message.requestId,
    message: `SESSION_INVALID: ${message.reason || "unknown"}`,
    description: "The app must keep access closed when the Gateway rejects the session."
  });
  addError(`Session invalid: ${message.reason || "unknown"}`);
}

function handleSessionLoggedOut(message) {
  state.result = {
    ...state.result,
    decision: "SESSION REVOKED",
    status: "logged out",
    sessionState: "logged out"
  };
  state.protocol = applySessionStatus(state.protocol, "idle", "SESSION_LOGGED_OUT");
  addEvent({
    type: "SESSION_LOGGED_OUT",
    stage: "SESSION_LOGOUT",
    requestId: message.requestId,
    message: "SESSION_LOGGED_OUT received.",
    description: "The server-side opaque session has been revoked."
  });
}

function handleGatewayError(message) {
  const readable = message.message || "ERROR received from Gateway.";
  state.result = {
    ...state.result,
    decision: "ACCESS DENIED",
    status: "error",
    success: false,
    serviceMessage: readable,
    errorType: message.errorType || null
  };
  state.protocol = applyFlowStage(state.protocol, "FLOW_ERROR", readable);
  addEvent({
    type: "ERROR",
    stage: message.errorType || "GATEWAY_ERROR",
    requestId: message.requestId,
    message: readable,
    description: "Gateway returned a typed error without exposing sensitive protocol data."
  });
  addError(readable);
}

function runSecurityScenario(kind) {
  if (!gateway.isConnected()) {
    addError("Connect the Gateway before running a live demo scenario.");
    render();
    return;
  }

  if (kind === "unknown-client") {
    startAuthFlow({
      prefix: "unknown-client",
      clientId: `unknown-client-${Date.now()}`,
      serviceId: state.inputs.serviceId || "1"
    });
    return;
  }

  if (kind === "unknown-service") {
    startAuthFlow({
      prefix: "unknown-service",
      clientId: state.inputs.clientId || "1",
      serviceId: `missing-service-${Date.now()}`
    });
    return;
  }

  if (kind === "invalid-session") {
    resetLiveFlow(createRequestId("invalid-session"));
    const invalidSessionId = `invalid-session-${Date.now()}`;
    state.flowRunning = false;
    state.activeStage = "VERIFY_SESSION";
    state.result = {
      ...createWaitingResult(),
      decision: "AUTHENTICATING",
      status: "verifying",
      requestId: state.activeRequestId,
      sessionId: invalidSessionId,
      sessionState: "verifying invalid session"
    };
    sendVerifySession(invalidSessionId, state.activeRequestId);
    render();
    return;
  }
}

function resetLiveFlow(requestId) {
  clearFlowTimeout();
  state.traceStartedAt = performance.now();
  state.events = [];
  state.errors = [];
  state.protocol = createProtocolState();
  state.activeRequestId = requestId;
  state.activeStage = "START_AUTH_FLOW";
  state.flowRunning = true;
}

function finishFlowWithError(message) {
  clearFlowTimeout();
  state.flowRunning = false;
  state.activeStage = "FLOW_ERROR";
  state.protocol = applyFlowStage(state.protocol, "FLOW_ERROR", message);
  state.result = {
    ...createWaitingResult(),
    decision: "ACCESS DENIED",
    status: "error",
    requestId: state.activeRequestId || "none",
    serviceMessage: message,
    success: false
  };
  addError(message);
}

function armFlowTimeout(requestId) {
  clearFlowTimeout();
  flowTimeoutId = window.setTimeout(() => {
    if (state.flowRunning && state.activeRequestId === requestId) {
      finishFlowWithError("Flow timeout: no FLOW_RESULT received.");
      render();
    }
  }, FLOW_TIMEOUT_MS);
}

function clearFlowTimeout() {
  if (flowTimeoutId !== null) {
    window.clearTimeout(flowTimeoutId);
    flowTimeoutId = null;
  }
}

function updateInputs(inputs) {
  state.inputs = {
    wsUrl: inputs.wsUrl || state.inputs.wsUrl,
    clientId: inputs.clientId || "1",
    serviceId: inputs.serviceId || "1"
  };
  state.environment.healthUrl = inferHealthUrl(state.inputs.wsUrl);
}

function clearTrace() {
  clearFlowTimeout();
  state.events = [];
  state.errors = [];
  state.protocol = createProtocolState();
  state.result = createWaitingResult();
  state.activeStage = "idle";
  state.activeRequestId = null;
  state.traceStartedAt = null;
  state.flowRunning = false;
  render();
}

function addEvent(event) {
  const now = performance.now();
  if (state.traceStartedAt === null) {
    state.traceStartedAt = now;
  }
  state.events = [
    ...state.events,
    {
      type: event.type,
      stage: event.stage,
      requestId: event.requestId,
      message: event.message,
      description: event.description,
      elapsedMs: now - state.traceStartedAt
    }
  ].slice(-120);
}

function addError(message) {
  state.errors = [{ message }, ...state.errors].slice(0, 30);
}

async function refreshGatewayHealth() {
  const healthUrl = state.environment.healthUrl;
  if (!healthUrl) {
    return;
  }
  try {
    const response = await fetch(healthUrl, { cache: "no-store" });
    if (!response.ok) {
      return;
    }
    const health = await response.json();
    state.environment.storageMode = health.storageMode || "unavailable";
    state.environment.mode = "not exposed";
    render();
  } catch {
    state.environment.storageMode = "unavailable";
    state.environment.mode = "not exposed";
  }
}

function inferHealthUrl(wsUrl) {
  try {
    const url = new URL(wsUrl);
    const protocol = url.protocol === "wss:" ? "https:" : "http:";
    const port = url.port === "2800" ? "2801" : "2801";
    return `${protocol}//${url.hostname}:${port}/health`;
  } catch {
    return "http://127.0.0.1:2801/health";
  }
}

function render() {
  renderer.render(state);
}
