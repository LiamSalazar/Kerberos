const SENSITIVE_PATTERN = /secret|key|ticket|cipher|crypto|envelope|payload|password|contrasen|contrase/i;

export function createRenderer(actions) {
  const refs = {
    form: document.querySelector("[data-control-panel]"),
    wsUrl: document.querySelector("#ws-url"),
    clientId: document.querySelector("#client-id"),
    serviceId: document.querySelector("#service-id"),
    connectionStatus: document.querySelector("[data-connection-status]"),
    connectionLabel: document.querySelector("[data-connection-label]"),
    events: document.querySelector("[data-events]"),
    eventCount: document.querySelector("[data-event-count]"),
    resultStatus: document.querySelector("[data-result-status]"),
    resultRequest: document.querySelector("[data-result-request]"),
    resultMessage: document.querySelector("[data-result-message]"),
    resultLatency: document.querySelector("[data-result-latency]"),
    errors: document.querySelector("[data-errors]"),
    errorCount: document.querySelector("[data-error-count]"),
    stageCards: Array.from(document.querySelectorAll("[data-stage]")),
    stageStatuses: Array.from(document.querySelectorAll("[data-stage-status]"))
  };

  refs.form.addEventListener("click", (event) => {
    const action = event.target?.dataset?.action;
    if (!action) {
      return;
    }

    if (action === "connect") {
      actions.connect(readInputs(refs));
    }
    if (action === "start-flow") {
      actions.startFlow(readInputs(refs));
    }
    if (action === "clear-events") {
      actions.clearEvents();
    }
  });

  return {
    render(state) {
      refs.connectionStatus.dataset.connectionStatus = state.connection.status;
      refs.connectionLabel.textContent = state.connection.label;
      refs.wsUrl.value = state.inputs.wsUrl;
      refs.clientId.value = state.inputs.clientId;
      refs.serviceId.value = state.inputs.serviceId;

      renderStages(refs, state.stages);
      renderEvents(refs, state.events);
      renderErrors(refs, state.errors);
      renderResult(refs, state.result);
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

function renderStages(refs, stages) {
  for (const card of refs.stageCards) {
    const stageId = card.dataset.stage;
    const status = stages[stageId] || "idle";
    card.dataset.status = status;
  }

  for (const statusNode of refs.stageStatuses) {
    const stageId = statusNode.dataset.stageStatus;
    statusNode.textContent = stages[stageId] || "idle";
  }
}

function renderEvents(refs, events) {
  refs.eventCount.textContent = `${events.length} ${events.length === 1 ? "event" : "events"}`;
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
  refs.resultStatus.textContent = result.status;
  refs.resultStatus.dataset.resultStatus = result.success === false ? "error" : result.success === true ? "success" : "waiting";
  refs.resultRequest.textContent = safeDisplay(result.requestId);
  refs.resultMessage.textContent = safeDisplay(result.serviceMessage);
  refs.resultLatency.textContent = safeDisplay(result.latency);
}

function createEventNode(event) {
  const item = document.createElement("li");
  item.className = "event-item";

  const meta = document.createElement("span");
  meta.className = "event-meta";
  meta.textContent = [event.type, event.stage, event.requestId].filter(Boolean).join(" | ");

  const message = document.createElement("strong");
  message.textContent = safeDisplay(event.message || "Gateway event received.");

  item.append(meta, message);
  return item;
}

function createErrorNode(error) {
  const item = document.createElement("li");
  item.textContent = safeDisplay(error.message || error);
  return item;
}

function safeDisplay(value) {
  if (value === null || value === undefined || value === "") {
    return "none";
  }
  const text = String(value);
  if (SENSITIVE_PATTERN.test(text)) {
    return "[sensitive detail omitted]";
  }
  return text;
}
