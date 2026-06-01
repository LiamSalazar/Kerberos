import { formatLatencyValue, formatTraceTime } from "./state.js";

const SENSITIVE_PATTERN = /\b(secret|private[_-]?key|ciphertext|password|passwd|token|payload|cryptoEnvelope)\b|(?:key|secret|password|token)\s*[:=]/i;
const LONG_BASE64_PATTERN = /\b[A-Za-z0-9+/]{48,}={0,2}\b/;

export function createRenderer(actions) {
  const refs = {
    form: document.querySelector("[data-control-panel]"),
    wsUrl: document.querySelector("#ws-url"),
    clientId: document.querySelector("#client-id"),
    serviceId: document.querySelector("#service-id"),
    connectionStatus: document.querySelector("[data-connection-status]"),
    connectionLabel: document.querySelector("[data-connection-label]"),
    storageMode: document.querySelector("[data-storage-mode]"),
    authMode: document.querySelector("[data-auth-mode]"),
    lastLatency: document.querySelector("[data-last-latency]"),
    sessionChip: document.querySelector("[data-session-chip]"),
    activeStage: document.querySelector("[data-active-stage]"),
    events: document.querySelector("[data-events]"),
    eventCount: document.querySelector("[data-event-count]"),
    resultStatus: document.querySelector("[data-result-status]"),
    decisionState: document.querySelector("[data-decision-state]"),
    resultRequest: document.querySelector("[data-result-request]"),
    resultMessage: document.querySelector("[data-result-message]"),
    resultSession: document.querySelector("[data-result-session]"),
    resultSessionExpiresAt: document.querySelector("[data-result-session-expires-at]"),
    resultSessionState: document.querySelector("[data-result-session-state]"),
    latencyAs: document.querySelector("[data-latency-as]"),
    latencyTgs: document.querySelector("[data-latency-tgs]"),
    latencyService: document.querySelector("[data-latency-service]"),
    latencyTotal: document.querySelector("[data-latency-total]"),
    errors: document.querySelector("[data-errors]"),
    errorCount: document.querySelector("[data-error-count]"),
    validationLab: document.querySelector("[data-validation-lab]"),
    nodeCards: Array.from(document.querySelectorAll("[data-node]")),
    nodeStatuses: Array.from(document.querySelectorAll("[data-node-status]")),
    nodeLatencies: Array.from(document.querySelectorAll("[data-node-latency]")),
    nodeEvents: Array.from(document.querySelectorAll("[data-node-event]")),
    linkNodes: Array.from(document.querySelectorAll("[data-link]")),
    messageCards: Array.from(document.querySelectorAll("[data-message]")),
    messageStatuses: Array.from(document.querySelectorAll("[data-message-status]")),
    actionButtons: Array.from(document.querySelectorAll("[data-action]"))
  };

  refs.form.addEventListener("submit", (event) => {
    event.preventDefault();
  });

  document.addEventListener("click", (event) => {
    const actionButton = event.target.closest("[data-action]");
    if (actionButton) {
      const action = actionButton.dataset.action;
      if (action === "connect") {
        actions.connect(readInputs(refs));
      }
      if (action === "start-flow") {
        actions.startFlow(readInputs(refs));
      }
      if (action === "verify-session") {
        actions.verifySession(readInputs(refs));
      }
      if (action === "logout-session") {
        actions.logoutSession();
      }
      if (action === "clear-events") {
        actions.clearEvents();
      }
      return;
    }

    const scenarioButton = event.target.closest("[data-scenario]");
    if (scenarioButton) {
      actions.runScenario(scenarioButton.dataset.scenario, readInputs(refs));
    }
  });

  return {
    render(state) {
      renderInputs(refs, state);
      renderHeader(refs, state);
      renderProtocol(refs, state.protocol);
      renderMessages(refs, state.protocol.messages);
      renderEvents(refs, state.events);
      renderErrors(refs, state.errors);
      renderResult(refs, state.result);
      renderValidationLab(refs, state.validations);
      renderButtons(refs, state);
    }
  };
}

function readInputs(refs) {
  return {
    wsUrl: refs.wsUrl.value.trim(),
    clientId: refs.clientId.value.trim(),
    serviceId: refs.serviceId.value.trim()
  };
}

function renderInputs(refs, state) {
  refs.wsUrl.value = state.inputs.wsUrl;
  refs.clientId.value = state.inputs.clientId;
  refs.serviceId.value = state.inputs.serviceId;
}

function renderHeader(refs, state) {
  refs.connectionStatus.dataset.connectionStatus = state.connection.status;
  refs.connectionLabel.textContent = state.connection.label;
  refs.storageMode.textContent = safeDisplay(state.environment.storageMode);
  refs.authMode.textContent = safeDisplay(state.environment.mode);
  refs.lastLatency.textContent = formatLatencyValue(state.result.latencies.total);
  refs.sessionChip.textContent = safeDisplay(state.result.sessionState);
  refs.activeStage.textContent = safeDisplay(state.activeStage);
}

function renderProtocol(refs, protocol) {
  for (const card of refs.nodeCards) {
    const node = protocol.nodes[card.dataset.node];
    card.dataset.status = node?.status || "idle";
  }

  for (const statusNode of refs.nodeStatuses) {
    const node = protocol.nodes[statusNode.dataset.nodeStatus];
    statusNode.textContent = node?.status || "idle";
  }

  for (const latencyNode of refs.nodeLatencies) {
    const node = protocol.nodes[latencyNode.dataset.nodeLatency];
    latencyNode.textContent = formatLatencyValue(node?.latency);
  }

  for (const eventNode of refs.nodeEvents) {
    const node = protocol.nodes[eventNode.dataset.nodeEvent];
    eventNode.textContent = safeDisplay(node?.event || "none");
  }

  for (const link of refs.linkNodes) {
    link.dataset.status = protocol.links[link.dataset.link] || "idle";
  }
}

function renderMessages(refs, messages) {
  for (const card of refs.messageCards) {
    card.dataset.status = messages[card.dataset.message] || "idle";
  }

  for (const statusNode of refs.messageStatuses) {
    const messageId = statusNode.dataset.messageStatus;
    statusNode.textContent = messages[messageId] || "idle";
  }
}

function renderEvents(refs, events) {
  refs.eventCount.textContent = `${events.length} ${events.length === 1 ? "event" : "events"}`;
  if (events.length === 0) {
    const empty = document.createElement("li");
    empty.className = "empty-state";
    empty.textContent = "[00:00.000] waiting for protocol activity";
    refs.events.replaceChildren(empty);
    return;
  }
  refs.events.replaceChildren(...events.map(createEventNode));
}

function renderErrors(refs, errors) {
  refs.errorCount.textContent = `${errors.length} ${errors.length === 1 ? "error" : "errors"}`;
  if (errors.length === 0) {
    const empty = document.createElement("li");
    empty.className = "empty-state";
    empty.textContent = "No errors reported.";
    refs.errors.replaceChildren(empty);
    return;
  }
  refs.errors.replaceChildren(...errors.map(createErrorNode));
}

function renderResult(refs, result) {
  refs.decisionState.textContent = result.decision;
  refs.decisionState.dataset.decision = result.decision.toLowerCase().replace(/\s+/g, "-");
  refs.resultStatus.textContent = result.status;
  refs.resultStatus.dataset.resultStatus = result.success === false ? "error" : result.success === true ? "success" : "waiting";
  refs.resultRequest.textContent = safeDisplay(result.requestId);
  refs.resultMessage.textContent = safeDisplay(result.serviceMessage);
  refs.resultSession.textContent = maskSessionId(result.sessionId);
  refs.resultSessionExpiresAt.textContent = safeDisplay(result.sessionExpiresAt);
  refs.resultSessionState.textContent = safeDisplay(result.sessionState);
  refs.latencyAs.textContent = formatLatencyValue(result.latencies.as);
  refs.latencyTgs.textContent = formatLatencyValue(result.latencies.tgs);
  refs.latencyService.textContent = formatLatencyValue(result.latencies.service);
  refs.latencyTotal.textContent = formatLatencyValue(result.latencies.total);
}

function renderValidationLab(refs, validations) {
  refs.validationLab.replaceChildren(...validations.map(createValidationCard));
}

function renderButtons(refs, state) {
  const connected = state.connection.status === "connected";
  const hasSession = Boolean(state.result.sessionId && state.result.sessionId !== "none");

  for (const button of refs.actionButtons) {
    const action = button.dataset.action;
    if (action === "start-flow") {
      button.disabled = !connected || state.flowRunning;
    }
    if (action === "verify-session" || action === "logout-session") {
      button.disabled = !connected || !hasSession;
    }
  }
}

function createEventNode(event) {
  const item = document.createElement("li");
  item.className = "event-item";

  const time = document.createElement("span");
  time.className = "event-time";
  time.textContent = formatTraceTime(event.elapsedMs);

  const meta = document.createElement("span");
  meta.className = "event-meta";
  meta.textContent = [event.type, event.stage, event.requestId].filter(Boolean).join(" | ");

  const message = document.createElement("strong");
  message.textContent = safeDisplay(event.message || "Gateway event received.");

  const description = document.createElement("p");
  description.textContent = safeDisplay(event.description || "Sensitive details omitted.");

  item.append(time, meta, message, description);
  return item;
}

function createErrorNode(error) {
  const item = document.createElement("li");
  item.textContent = safeDisplay(error.message || error);
  return item;
}

function createValidationCard(validation) {
  const card = document.createElement("article");
  card.className = "validation-card";
  card.dataset.validationKind = validation.kind;

  const header = document.createElement("div");
  header.className = "validation-card-header";
  const title = document.createElement("h3");
  title.textContent = validation.title;
  const tag = document.createElement("span");
  tag.textContent = validation.type;
  header.append(title, tag);

  const expected = document.createElement("p");
  expected.className = "validation-expected";
  expected.textContent = `Expected: ${validation.expected}`;

  const flow = document.createElement("p");
  flow.textContent = validation.flow;

  const explanation = document.createElement("p");
  explanation.textContent = validation.explanation;

  const evidence = document.createElement("strong");
  evidence.textContent = validation.evidence;

  card.append(header, expected, flow, explanation, evidence);

  if (validation.scenario) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "button mini";
    button.dataset.scenario = validation.scenario;
    button.textContent = validation.buttonLabel;
    card.append(button);
  }

  return card;
}

function safeDisplay(value) {
  if (value === null || value === undefined || value === "") {
    return "none";
  }
  const text = String(value);
  if (SENSITIVE_PATTERN.test(text) || LONG_BASE64_PATTERN.test(text)) {
    return "[sensitive detail omitted]";
  }
  return text;
}

function maskSessionId(sessionId) {
  if (!sessionId || sessionId === "none") {
    return "none";
  }
  if (sessionId.length < 12) {
    return "masked";
  }
  return `${sessionId.slice(0, 6)}...${sessionId.slice(-4)}`;
}
