const form = document.querySelector("#login-form");
const loginPanel = document.querySelector("#login-panel");
const dashboard = document.querySelector("#dashboard");
const loginButton = document.querySelector("#login-button");
const gatewayUrl = document.querySelector("#gateway-url");
const clientId = document.querySelector("#client-id");
const serviceId = document.querySelector("#service-id");
const statusLine = document.querySelector("#status-line");
const errorLine = document.querySelector("#error-line");
const eventLog = document.querySelector("#event-log");
const logoutButton = document.querySelector("#logout-button");
const clearEventsButton = document.querySelector("#clear-events-button");
const requestIdView = document.querySelector("#request-id");
const serviceMessageView = document.querySelector("#service-message");
const authenticatedState = document.querySelector("#authenticated-state");
const sessionStateView = document.querySelector("#session-state");
const sessionIdView = document.querySelector("#session-id");
const sessionExpiresAtView = document.querySelector("#session-expires-at");
const gatewayState = document.querySelector("#gateway-state");
const authState = document.querySelector("#auth-state");
const technicalSession = document.querySelector("#technical-session");
const accessState = document.querySelector("#access-state");
const steps = Array.from(document.querySelectorAll("[data-step]"));

let socket = null;
let activeRequestId = null;
let activeSessionId = null;
let activeSessionExpiresAt = null;
let activeClientId = null;
let activeServiceId = null;
let flowTimeout = null;
let logoutPending = false;
let traceStartedAt = null;

form.addEventListener("submit", (event) => {
  event.preventDefault();
  authenticate();
});

logoutButton.addEventListener("click", () => {
  logout();
});

clearEventsButton.addEventListener("click", () => {
  clearEvents();
});

function authenticate() {
  closeSocket();
  clearEvents();
  clearError();
  resetSteps();
  setStatus("Connecting");
  setTechnicalState({
    gateway: "connecting",
    auth: "connecting",
    session: "not issued",
    access: "closed"
  });
  setStep("gateway", "active");
  loginButton.disabled = true;

  activeRequestId = `sample-login-${Date.now()}`;
  activeSessionId = null;
  activeSessionExpiresAt = null;
  activeClientId = clientId.value.trim();
  activeServiceId = serviceId.value.trim();
  logoutPending = false;
  traceStartedAt = performance.now();
  socket = new WebSocket(gatewayUrl.value.trim());

  socket.addEventListener("open", () => {
    setStatus("Authenticating");
    setTechnicalState({ gateway: "connected", auth: "authenticating" });
    setStep("gateway", "success");
    sendStartFlow();
  });

  socket.addEventListener("message", (event) => {
    handleGatewayMessage(event.data);
  });

  socket.addEventListener("error", () => {
    fail("Gateway connection failed");
  });

  socket.addEventListener("close", () => {
    if (!dashboard.hidden) {
      return;
    }
    loginButton.disabled = false;
    if (statusLine.textContent !== "Access denied") {
      setStatus("Disconnected");
      setTechnicalState({ gateway: "disconnected", auth: "idle" });
    }
  });

  armTimeout("Authentication timed out");
}

function sendStartFlow() {
  const message = {
    type: "START_AUTH_FLOW",
    requestId: activeRequestId,
    clientId: activeClientId,
    serviceId: activeServiceId
  };
  socket.send(JSON.stringify(message));
  setStep("start", "active");
  addEvent("START_AUTH_FLOW", "Sent to Gateway; AS/TGS/Service remain behind it.");
}

function sendVerifySession() {
  const message = {
    type: "VERIFY_SESSION",
    requestId: `${activeRequestId}-verify`,
    sessionId: activeSessionId,
    clientId: activeClientId,
    serviceId: activeServiceId
  };
  socket.send(JSON.stringify(message));
  setStatus("Verifying session");
  setSessionState("verifying session");
  setTechnicalState({ auth: "verifying session", session: "verification pending" });
  setStep("verify", "active");
  addEvent("VERIFY_SESSION", "Sent to Gateway before opening the protected dashboard.");
  armTimeout("Session verification timed out");
}

function sendLogoutSession() {
  const message = {
    type: "LOGOUT_SESSION",
    requestId: `${activeRequestId || "sample-login"}-logout`,
    sessionId: activeSessionId
  };
  socket.send(JSON.stringify(message));
  setStatus("Logging out");
  setSessionState("logging out");
  setTechnicalState({ session: "logging out", access: "closing" });
  addEvent("LOGOUT_SESSION", "Sent to Gateway for server-side revocation.");
  armTimeout("Logout timed out");
}

function handleGatewayMessage(rawMessage) {
  let message;
  try {
    message = JSON.parse(rawMessage);
  } catch {
    fail("Gateway returned invalid JSON");
    return;
  }

  if (message.type === "FLOW_EVENT") {
    handleFlowEvent(message);
    return;
  }

  if (message.type === "FLOW_RESULT") {
    window.clearTimeout(flowTimeout);
    if (message.success === true) {
      if (!message.sessionId) {
        fail("Gateway did not return a verifiable session");
        closeSocket();
        return;
      }
      activeSessionId = message.sessionId;
      activeSessionExpiresAt = message.sessionExpiresAt || "not available";
      serviceMessageView.textContent = safeDisplay(message.serviceMessage || "Access granted");
      sessionIdView.textContent = maskSessionId(activeSessionId);
      sessionExpiresAtView.textContent = activeSessionExpiresAt;
      setTechnicalState({ auth: "session issued", session: "issued; verify required" });
      addEvent("FLOW_RESULT", "Opaque session issued; dashboard still locked.");
      sendVerifySession();
    } else {
      fail(message.serviceMessage || message.errorType || "Authentication failed");
      closeSocket();
    }
    return;
  }

  if (message.type === "SESSION_VALID") {
    window.clearTimeout(flowTimeout);
    loginButton.disabled = false;
    grantAccess(message);
    return;
  }

  if (message.type === "SESSION_INVALID") {
    fail(`Session invalid: ${message.reason || "INVALID"}`);
    closeSocket();
    return;
  }

  if (message.type === "SESSION_LOGGED_OUT") {
    window.clearTimeout(flowTimeout);
    addEvent("SESSION_LOGGED_OUT", "Gateway revoked the session.");
    resetAuthState();
    closeSocket();
    return;
  }

  if (message.type === "ERROR") {
    if (logoutPending) {
      addEvent("LOGOUT_ERROR", message.message || "Gateway logout error");
      resetAuthState();
      closeSocket();
      return;
    }
    fail(message.message || "Gateway error");
    closeSocket();
  }
}

function handleFlowEvent(message) {
  const stage = message.stage || "FLOW_EVENT";
  addEvent(stage, safeDisplay(message.message || ""));

  if (stage === "FLOW_STARTED") {
    setStep("start", "success");
  }
  if (stage === "AS_REQUEST_SENT") {
    setStep("as", "active");
  }
  if (stage === "AS_RESPONSE_RECEIVED") {
    setStep("as", "success");
  }
  if (stage === "TGS_REQUEST_SENT") {
    setStep("tgs", "active");
  }
  if (stage === "TGS_RESPONSE_RECEIVED") {
    setStep("tgs", "success");
  }
  if (stage === "SERVICE_REQUEST_SENT") {
    setStep("service", "active");
  }
  if (stage === "SERVICE_RESPONSE_RECEIVED") {
    setStep("service", "success");
  }
  if (stage === "FLOW_ERROR") {
    fail(message.message || "Authentication flow failed");
  }
}

function grantAccess(message) {
  setStatus("Access granted");
  setSessionState("session valid");
  setTechnicalState({
    auth: "verified",
    session: "valid",
    access: "granted"
  });
  setStep("verify", "success");
  clearError();
  loginPanel.hidden = true;
  dashboard.hidden = false;
  requestIdView.textContent = activeRequestId;
  authenticatedState.textContent = "true";
  sessionExpiresAtView.textContent = message.expiresAt || activeSessionExpiresAt || "not available";
  addEvent("SESSION_VALID", "Protected dashboard unlocked.");
}

function fail(message) {
  window.clearTimeout(flowTimeout);
  loginButton.disabled = false;
  dashboard.hidden = true;
  loginPanel.hidden = false;
  authenticatedState.textContent = "false";
  setSessionState("session invalid");
  setStatus("Access denied");
  setTechnicalState({
    auth: "denied",
    session: activeSessionId ? "invalid" : "not issued",
    access: "denied"
  });
  setActiveStepError();
  errorLine.textContent = safeDisplay(message);
  errorLine.hidden = false;
  addEvent("ACCESS_DENIED", safeDisplay(message));
}

function logout() {
  if (activeSessionId && socket?.readyState === WebSocket.OPEN) {
    logoutPending = true;
    sendLogoutSession();
    return;
  }
  resetAuthState();
  closeSocket();
}

function resetAuthState() {
  window.clearTimeout(flowTimeout);
  activeRequestId = null;
  activeSessionId = null;
  activeSessionExpiresAt = null;
  activeClientId = null;
  activeServiceId = null;
  logoutPending = false;
  loginButton.disabled = false;
  dashboard.hidden = true;
  loginPanel.hidden = false;
  authenticatedState.textContent = "false";
  requestIdView.textContent = "-";
  serviceMessageView.textContent = "-";
  sessionIdView.textContent = "-";
  sessionExpiresAtView.textContent = "-";
  setSessionState("logged out");
  setStatus("Disconnected");
  setTechnicalState({
    gateway: "disconnected",
    auth: "idle",
    session: "logged out",
    access: "closed"
  });
  resetSteps();
}

function closeSocket() {
  window.clearTimeout(flowTimeout);
  if (socket && socket.readyState <= WebSocket.OPEN) {
    socket.close();
  }
  socket = null;
}

function addEvent(title, message) {
  const item = document.createElement("li");
  const time = document.createElement("span");
  const strong = document.createElement("strong");
  const detail = document.createElement("p");
  time.textContent = formatTraceTime();
  strong.textContent = safeDisplay(title);
  detail.textContent = safeDisplay(message);
  item.append(time, strong, detail);
  eventLog.append(item);
  eventLog.scrollTop = eventLog.scrollHeight;
}

function clearEvents() {
  eventLog.replaceChildren();
  traceStartedAt = performance.now();
}

function clearError() {
  errorLine.hidden = true;
  errorLine.textContent = "";
}

function setStatus(message) {
  statusLine.textContent = message;
}

function setSessionState(message) {
  sessionStateView.textContent = message;
}

function setTechnicalState(next) {
  if (next.gateway !== undefined) {
    gatewayState.textContent = next.gateway;
    gatewayState.dataset.state = normalizeState(next.gateway);
  }
  if (next.auth !== undefined) {
    authState.textContent = next.auth;
    authState.dataset.state = normalizeState(next.auth);
  }
  if (next.session !== undefined) {
    technicalSession.textContent = next.session;
    technicalSession.dataset.state = normalizeState(next.session);
  }
  if (next.access !== undefined) {
    accessState.textContent = next.access;
    accessState.dataset.state = normalizeState(next.access);
  }
}

function setStep(stepId, status) {
  const step = steps.find((item) => item.dataset.step === stepId);
  if (step) {
    step.dataset.status = status;
  }
}

function resetSteps() {
  for (const step of steps) {
    step.dataset.status = "idle";
  }
}

function setActiveStepError() {
  const active = steps.find((step) => step.dataset.status === "active");
  if (active) {
    active.dataset.status = "error";
  }
}

function armTimeout(message) {
  window.clearTimeout(flowTimeout);
  flowTimeout = window.setTimeout(() => {
    fail(message);
    closeSocket();
  }, 12000);
}

function formatTraceTime() {
  if (traceStartedAt === null) {
    traceStartedAt = performance.now();
  }
  const elapsed = Math.max(0, Math.floor(performance.now() - traceStartedAt));
  const seconds = Math.floor(elapsed / 1000);
  const millis = elapsed % 1000;
  return `[${String(seconds).padStart(2, "0")}.${String(millis).padStart(3, "0")}]`;
}

function normalizeState(value) {
  if (/granted|valid|connected|verified|issued/i.test(value)) {
    return "success";
  }
  if (/denied|invalid|failed|disconnected/i.test(value)) {
    return "error";
  }
  if (/connecting|authenticating|verifying|pending|logging/i.test(value)) {
    return "active";
  }
  return "idle";
}

function maskSessionId(sessionId) {
  if (!sessionId || sessionId.length < 12) {
    return "masked";
  }
  return `${sessionId.slice(0, 6)}...${sessionId.slice(-4)}`;
}

function safeDisplay(value) {
  if (value === null || value === undefined || value === "") {
    return "none";
  }
  const text = String(value);
  if (/\b(secret|private[_-]?key|ciphertext|password|passwd|token|payload|cryptoEnvelope)\b|(?:key|secret|password|token)\s*[:=]/i.test(text)) {
    return "[sensitive detail omitted]";
  }
  return text;
}
