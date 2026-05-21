import {
  applyFlowStage,
  createIdleStages,
  createInitialState,
  createRequestId,
  formatLatency
} from "./state.js";
import { createRenderer } from "./renderer.js";
import { GatewayWebSocketClient, assertWebSocketSupport } from "./websocket-client.js";

const FLOW_TIMEOUT_MS = 15_000;

const state = createInitialState();
let flowTimeoutId = null;

const gateway = new GatewayWebSocketClient({
  onOpen() {
    state.connection.status = "connected";
    state.connection.label = "Connected";
    addEvent({ type: "LOCAL", message: "WebSocket conectado." });
    render();
  },
  onClose() {
    state.connection.status = "closed";
    state.connection.label = "Disconnected";
    if (state.flowRunning) {
      finishFlowWithError("WebSocket cerrado antes de recibir FLOW_RESULT.");
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
  clearEvents() {
    clearEvents();
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
    addError("WebSocket URL requerida.");
    render();
    return;
  }

  state.connection.status = "connecting";
  state.connection.label = "Connecting";
  state.connection.url = state.inputs.wsUrl;
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

function startAuthFlow() {
  if (!gateway.isConnected()) {
    addError("Gateway no disponible. Conecta el WebSocket antes de iniciar el flujo.");
    render();
    return;
  }

  const requestId = createRequestId();
  state.activeRequestId = requestId;
  state.flowRunning = true;
  state.stages = createIdleStages();
  state.result = {
    status: "running",
    requestId,
    serviceMessage: "Waiting for FLOW_RESULT.",
    latency: "not available",
    success: null
  };
  addEvent({
    type: "LOCAL",
    requestId,
    message: "START_AUTH_FLOW enviado al gateway."
  });
  render();

  try {
    gateway.send({
      type: "START_AUTH_FLOW",
      requestId,
      clientId: state.inputs.clientId || "1",
      serviceId: state.inputs.serviceId || "1"
    });
    armFlowTimeout(requestId);
  } catch (error) {
    finishFlowWithError(error.message);
  }
}

function handleGatewayMessage(message) {
  switch (message.type) {
    case "GATEWAY_READY":
      state.connection.status = "ready";
      state.connection.label = "Gateway ready";
      addEvent({
        type: "GATEWAY_READY",
        message: message.message || "Gateway listo."
      });
      break;
    case "FLOW_EVENT":
      handleFlowEvent(message);
      break;
    case "FLOW_RESULT":
      handleFlowResult(message);
      break;
    case "ERROR":
      addError(message.message || "Respuesta ERROR recibida desde el gateway.");
      break;
    case "PONG":
      addEvent({
        type: "PONG",
        requestId: message.requestId,
        message: message.message || "pong"
      });
      break;
    default:
      addError(`Tipo de mensaje WebSocket desconocido: ${message.type || "none"}`);
      break;
  }
  render();
}

function handleFlowEvent(message) {
  const stage = message.stage || "FLOW_EVENT";
  state.stages = applyFlowStage(state.stages, stage);
  addEvent({
    type: message.type,
    stage,
    requestId: message.requestId,
    message: message.message || stage
  });

  if (stage === "FLOW_ERROR") {
    state.flowRunning = false;
  }
}

function handleFlowResult(message) {
  clearFlowTimeout();
  state.flowRunning = false;
  state.result = {
    status: message.success ? "success" : "error",
    requestId: message.requestId || state.activeRequestId || "none",
    serviceMessage: message.serviceMessage || "No service message returned.",
    latency: formatLatency(message),
    success: Boolean(message.success)
  };

  if (message.success) {
    state.stages = applyFlowStage(state.stages, "FLOW_SUCCESS");
  } else {
    state.stages = applyFlowStage(state.stages, "FLOW_ERROR");
    addError(message.serviceMessage || "El flujo termino con error.");
  }
}

function finishFlowWithError(message) {
  clearFlowTimeout();
  state.flowRunning = false;
  state.stages = applyFlowStage(state.stages, "FLOW_ERROR");
  state.result = {
    status: "error",
    requestId: state.activeRequestId || "none",
    serviceMessage: message,
    latency: "not available",
    success: false
  };
  addError(message);
}

function armFlowTimeout(requestId) {
  clearFlowTimeout();
  flowTimeoutId = window.setTimeout(() => {
    if (state.flowRunning && state.activeRequestId === requestId) {
      finishFlowWithError("Timeout de flujo: no llego FLOW_RESULT.");
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
}

function clearEvents() {
  state.events = [];
  state.errors = [];
  state.stages = createIdleStages();
  state.result = {
    status: "waiting",
    requestId: "none",
    serviceMessage: "Run a flow to see the protected response.",
    latency: "not available",
    success: null
  };
  render();
}

function addEvent(event) {
  state.events = [
    {
      type: event.type,
      stage: event.stage,
      requestId: event.requestId,
      message: event.message
    },
    ...state.events
  ].slice(0, 80);
}

function addError(message) {
  state.errors = [{ message }, ...state.errors].slice(0, 30);
}

function render() {
  renderer.render(state);
}
