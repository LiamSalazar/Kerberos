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
const requestIdView = document.querySelector("#request-id");
const serviceMessageView = document.querySelector("#service-message");
const authenticatedState = document.querySelector("#authenticated-state");
const sessionStateView = document.querySelector("#session-state");
const sessionIdView = document.querySelector("#session-id");
const sessionExpiresAtView = document.querySelector("#session-expires-at");

let socket = null;
let activeRequestId = null;
let activeSessionId = null;
let activeSessionExpiresAt = null;
let activeClientId = null;
let activeServiceId = null;
let flowTimeout = null;
let logoutPending = false;

form.addEventListener("submit", (event) => {
  event.preventDefault();
  authenticate();
});

logoutButton.addEventListener("click", () => {
  logout();
});

function authenticate() {
  closeSocket();
  clearEvents();
  clearError();
  setStatus("Connecting");
  loginButton.disabled = true;

  activeRequestId = `sample-login-${Date.now()}`;
  activeSessionId = null;
  activeSessionExpiresAt = null;
  activeClientId = clientId.value.trim();
  activeServiceId = serviceId.value.trim();
  logoutPending = false;
  socket = new WebSocket(gatewayUrl.value.trim());

  socket.addEventListener("open", () => {
    setStatus("Authenticating");
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
    }
  });

  flowTimeout = window.setTimeout(() => {
    fail("Authentication timed out");
    closeSocket();
  }, 12000);
}

function sendStartFlow() {
  const message = {
    type: "START_AUTH_FLOW",
    requestId: activeRequestId,
    clientId: activeClientId,
    serviceId: activeServiceId,
  };
  socket.send(JSON.stringify(message));
  addEvent("START_AUTH_FLOW", "Sent to gateway");
}

function sendVerifySession() {
  const message = {
    type: "VERIFY_SESSION",
    requestId: `${activeRequestId}-verify`,
    sessionId: activeSessionId,
    clientId: activeClientId,
    serviceId: activeServiceId,
  };
  socket.send(JSON.stringify(message));
  setStatus("Verifying session");
  setSessionState("verifying session");
  addEvent("VERIFY_SESSION", "Sent to gateway");
  armTimeout("Session verification timed out");
}

function sendLogoutSession() {
  const message = {
    type: "LOGOUT_SESSION",
    requestId: `${activeRequestId || "sample-login"}-logout`,
    sessionId: activeSessionId,
  };
  socket.send(JSON.stringify(message));
  setStatus("Logging out");
  setSessionState("logging out");
  addEvent("LOGOUT_SESSION", "Sent to gateway");
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
    addEvent(message.stage || "FLOW_EVENT", message.message || "");
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
      serviceMessageView.textContent = message.serviceMessage || "Access granted";
      sessionIdView.textContent = maskSessionId(activeSessionId);
      sessionExpiresAtView.textContent = activeSessionExpiresAt;
      addEvent("FLOW_RESULT", "Session issued by gateway");
      sendVerifySession();
    } else {
      fail(message.serviceMessage || "Authentication failed");
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
    addEvent("SESSION_LOGGED_OUT", "Gateway revoked the session");
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

function grantAccess(message) {
  setStatus("Access granted");
  setSessionState("session valid");
  clearError();
  loginPanel.hidden = true;
  dashboard.hidden = false;
  requestIdView.textContent = activeRequestId;
  authenticatedState.textContent = "true";
  sessionExpiresAtView.textContent = message.expiresAt || activeSessionExpiresAt || "not available";
  addEvent("SESSION_VALID", "Protected dashboard unlocked");
}

function fail(message) {
  window.clearTimeout(flowTimeout);
  loginButton.disabled = false;
  dashboard.hidden = true;
  loginPanel.hidden = false;
  authenticatedState.textContent = "false";
  setSessionState("session invalid");
  setStatus("Access denied");
  errorLine.textContent = message;
  errorLine.hidden = false;
  addEvent("ACCESS_DENIED", message);
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
  const strong = document.createElement("strong");
  strong.textContent = title;
  item.append(strong, ` ${message}`);
  eventLog.prepend(item);
}

function clearEvents() {
  eventLog.replaceChildren();
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

function armTimeout(message) {
  window.clearTimeout(flowTimeout);
  flowTimeout = window.setTimeout(() => {
    fail(message);
    closeSocket();
  }, 12000);
}

function maskSessionId(sessionId) {
  if (!sessionId || sessionId.length < 12) {
    return "masked";
  }
  return `${sessionId.slice(0, 6)}...${sessionId.slice(-4)}`;
}
