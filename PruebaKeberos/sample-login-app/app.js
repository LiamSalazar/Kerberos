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

let socket = null;
let activeRequestId = null;
let flowTimeout = null;

form.addEventListener("submit", (event) => {
  event.preventDefault();
  authenticate();
});

logoutButton.addEventListener("click", () => {
  closeSocket();
  activeRequestId = null;
  dashboard.hidden = true;
  loginPanel.hidden = false;
  authenticatedState.textContent = "false";
  setStatus("Disconnected");
});

function authenticate() {
  closeSocket();
  clearEvents();
  clearError();
  setStatus("Connecting");
  loginButton.disabled = true;

  activeRequestId = `sample-login-${Date.now()}`;
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
    clientId: clientId.value.trim(),
    serviceId: serviceId.value.trim(),
  };
  socket.send(JSON.stringify(message));
  addEvent("START_AUTH_FLOW", "Sent to gateway");
}

function handleGatewayMessage(rawMessage) {
  let message;
  try {
    message = JSON.parse(rawMessage);
  } catch {
    fail("Gateway returned invalid JSON");
    return;
  }

  if (message.type === "GATEWAY_READY") {
    addEvent("GATEWAY_READY", message.message || "Ready");
    return;
  }

  if (message.type === "FLOW_EVENT") {
    addEvent(message.stage || "FLOW_EVENT", message.message || "");
    return;
  }

  if (message.type === "FLOW_RESULT") {
    window.clearTimeout(flowTimeout);
    loginButton.disabled = false;
    if (message.success === true) {
      grantAccess(message);
    } else {
      fail(message.serviceMessage || "Authentication failed");
    }
    closeSocket();
    return;
  }

  if (message.type === "ERROR") {
    fail(message.message || "Gateway error");
  }
}

function grantAccess(message) {
  setStatus("Access granted");
  clearError();
  loginPanel.hidden = true;
  dashboard.hidden = false;
  requestIdView.textContent = message.requestId || activeRequestId;
  serviceMessageView.textContent = message.serviceMessage || "Access granted";
  authenticatedState.textContent = "true";
  addEvent("ACCESS_GRANTED", "Protected dashboard unlocked");
}

function fail(message) {
  window.clearTimeout(flowTimeout);
  loginButton.disabled = false;
  dashboard.hidden = true;
  loginPanel.hidden = false;
  authenticatedState.textContent = "false";
  setStatus("Access denied");
  errorLine.textContent = message;
  errorLine.hidden = false;
  addEvent("ACCESS_DENIED", message);
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
