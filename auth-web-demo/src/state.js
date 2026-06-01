export const NODE_DEFINITIONS = [
  { id: "client", label: "Client App" },
  { id: "gateway", label: "WebSocket Gateway" },
  { id: "as", label: "Authentication Server" },
  { id: "tgs", label: "Ticket Granting Server" },
  { id: "service", label: "Protected Service" },
  { id: "session", label: "Opaque Session" }
];

export const LINK_DEFINITIONS = [
  "client-gateway",
  "gateway-as",
  "as-tgs",
  "tgs-service",
  "gateway-session"
];

export const MESSAGE_IDS = [
  "start_auth_flow",
  "as_request",
  "as_response",
  "tgs_request",
  "tgs_response",
  "service_request",
  "service_response",
  "flow_result",
  "verify_session",
  "session_valid"
];

const STAGE_PATCHES = {
  FLOW_STARTED: {
    nodes: {
      client: "processing",
      gateway: "processing",
      as: "waiting",
      tgs: "waiting",
      service: "waiting",
      session: "idle"
    },
    links: { "client-gateway": "active" },
    messages: { start_auth_flow: "active" }
  },
  AS_REQUEST_SENT: {
    nodes: { gateway: "processing", as: "processing" },
    links: { "client-gateway": "success", "gateway-as": "active" },
    messages: { start_auth_flow: "success", as_request: "active" }
  },
  AS_RESPONSE_RECEIVED: {
    nodes: { as: "success", tgs: "waiting" },
    links: { "gateway-as": "success", "as-tgs": "active" },
    messages: { as_request: "success", as_response: "success" }
  },
  TGS_REQUEST_SENT: {
    nodes: { tgs: "processing" },
    links: { "as-tgs": "active" },
    messages: { tgs_request: "active" }
  },
  TGS_RESPONSE_RECEIVED: {
    nodes: { tgs: "success", service: "waiting" },
    links: { "as-tgs": "success", "tgs-service": "active" },
    messages: { tgs_request: "success", tgs_response: "success" }
  },
  SERVICE_REQUEST_SENT: {
    nodes: { service: "processing" },
    links: { "tgs-service": "active" },
    messages: { service_request: "active" }
  },
  SERVICE_RESPONSE_RECEIVED: {
    nodes: { service: "success", gateway: "processing" },
    links: { "tgs-service": "success" },
    messages: { service_request: "success", service_response: "success" }
  },
  FLOW_SUCCESS: {
    nodes: {
      client: "success",
      gateway: "success",
      as: "success",
      tgs: "success",
      service: "success",
      session: "waiting"
    },
    links: {
      "client-gateway": "success",
      "gateway-as": "success",
      "as-tgs": "success",
      "tgs-service": "success"
    },
    messages: { flow_result: "active" }
  }
};

const STAGE_NODE_TOUCHES = {
  FLOW_STARTED: ["client", "gateway"],
  AS_REQUEST_SENT: ["gateway", "as"],
  AS_RESPONSE_RECEIVED: ["as", "gateway"],
  TGS_REQUEST_SENT: ["gateway", "tgs"],
  TGS_RESPONSE_RECEIVED: ["tgs", "gateway"],
  SERVICE_REQUEST_SENT: ["gateway", "service"],
  SERVICE_RESPONSE_RECEIVED: ["service", "gateway"],
  FLOW_SUCCESS: ["client", "gateway", "service"],
  FLOW_ERROR: ["client", "gateway"]
};

const STAGE_DESCRIPTIONS = {
  FLOW_STARTED: "Flow accepted by the Gateway.",
  AS_REQUEST_SENT: "Gateway/AuthClient sent the AS request.",
  AS_RESPONSE_RECEIVED: "AS response received; sensitive material omitted.",
  TGS_REQUEST_SENT: "Gateway/AuthClient requested service access from TGS.",
  TGS_RESPONSE_RECEIVED: "TGS response received; service ticket remains hidden.",
  SERVICE_REQUEST_SENT: "Gateway/AuthClient sent the protected service request.",
  SERVICE_RESPONSE_RECEIVED: "Protected service returned its access decision.",
  FLOW_SUCCESS: "Flow completed and the Gateway can issue an opaque session.",
  FLOW_ERROR: "Flow stopped with a controlled error."
};

export function createInitialState(validations = []) {
  return {
    connection: {
      status: "closed",
      label: "Disconnected",
      url: "ws://127.0.0.1:2800"
    },
    environment: {
      storageMode: "unavailable",
      mode: "not exposed",
      healthUrl: "http://127.0.0.1:2801/health"
    },
    inputs: {
      wsUrl: "ws://127.0.0.1:2800",
      clientId: "1",
      serviceId: "1"
    },
    protocol: createProtocolState(),
    events: [],
    errors: [],
    validations,
    result: createWaitingResult(),
    activeRequestId: null,
    activeStage: "idle",
    traceStartedAt: null,
    flowRunning: false
  };
}

export function createWaitingResult() {
  return {
    decision: "WAITING",
    status: "waiting",
    requestId: "none",
    serviceMessage: "Run a flow to see the protected response.",
    sessionId: "none",
    sessionExpiresAt: "not available",
    sessionState: "not issued",
    success: null,
    errorType: null,
    latencies: {
      as: null,
      tgs: null,
      service: null,
      total: null
    }
  };
}

export function createProtocolState() {
  return {
    nodes: NODE_DEFINITIONS.reduce((nodes, node) => {
      nodes[node.id] = {
        status: "idle",
        latency: null,
        event: "none"
      };
      return nodes;
    }, {}),
    links: LINK_DEFINITIONS.reduce((links, id) => {
      links[id] = "idle";
      return links;
    }, {}),
    messages: MESSAGE_IDS.reduce((messages, id) => {
      messages[id] = "idle";
      return messages;
    }, {})
  };
}

export function applyFlowStage(protocol, stageName, message = "") {
  if (stageName === "FLOW_ERROR") {
    return applyFlowError(protocol, message);
  }

  const patch = STAGE_PATCHES[stageName];
  if (!patch) {
    return protocol;
  }
  return applyProtocolPatch(protocol, patch, stageName, message);
}

export function applyMessageStatus(protocol, messageId, status) {
  if (!MESSAGE_IDS.includes(messageId)) {
    return protocol;
  }
  return {
    ...protocol,
    messages: {
      ...protocol.messages,
      [messageId]: status
    }
  };
}

export function applySessionStatus(protocol, status, eventLabel = "SESSION_VALID") {
  return {
    ...protocol,
    nodes: {
      ...protocol.nodes,
      session: {
        ...protocol.nodes.session,
        status,
        event: eventLabel
      }
    },
    links: {
      ...protocol.links,
      "gateway-session": status === "success" ? "success" : status === "error" ? "error" : "active"
    }
  };
}

export function applyResultLatencies(protocol, latencies) {
  return {
    ...protocol,
    nodes: {
      ...protocol.nodes,
      as: { ...protocol.nodes.as, latency: latencies.as },
      tgs: { ...protocol.nodes.tgs, latency: latencies.tgs },
      service: { ...protocol.nodes.service, latency: latencies.service },
      gateway: { ...protocol.nodes.gateway, latency: latencies.total }
    }
  };
}

export function createRequestId(prefix = "web") {
  const suffix = Math.random().toString(16).slice(2, 8);
  return `${prefix}-${Date.now()}-${suffix}`;
}

export function latencyParts(message) {
  return {
    as: numberOrNull(message.asMillis),
    tgs: numberOrNull(message.tgsMillis),
    service: numberOrNull(message.serviceMillis),
    total: numberOrNull(message.totalMillis)
  };
}

export function formatLatencyValue(value) {
  return Number.isFinite(value) ? `${value} ms` : "not available";
}

export function formatTraceTime(elapsedMs) {
  const safeMs = Math.max(0, Math.floor(elapsedMs || 0));
  const minutes = Math.floor(safeMs / 60000);
  const seconds = Math.floor((safeMs % 60000) / 1000);
  const millis = safeMs % 1000;
  return `[${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}.${String(millis).padStart(3, "0")}]`;
}

export function stageDescription(stageName) {
  return STAGE_DESCRIPTIONS[stageName] || "Gateway event received.";
}

function applyProtocolPatch(protocol, patch, stageName, message) {
  const nodes = { ...protocol.nodes };
  const links = { ...protocol.links };
  const messages = { ...protocol.messages };

  for (const [nodeId, status] of Object.entries(patch.nodes || {})) {
    nodes[nodeId] = {
      ...nodes[nodeId],
      status,
      event: STAGE_NODE_TOUCHES[stageName]?.includes(nodeId)
        ? stageName
        : nodes[nodeId]?.event || "none"
    };
  }

  for (const nodeId of STAGE_NODE_TOUCHES[stageName] || []) {
    if (nodes[nodeId]) {
      nodes[nodeId] = {
        ...nodes[nodeId],
        event: message || stageName
      };
    }
  }

  for (const [linkId, status] of Object.entries(patch.links || {})) {
    links[linkId] = status;
  }

  for (const [messageId, status] of Object.entries(patch.messages || {})) {
    messages[messageId] = status;
  }

  return { nodes, links, messages };
}

function applyFlowError(protocol, message) {
  const nodes = { ...protocol.nodes };
  for (const [nodeId, node] of Object.entries(nodes)) {
    if (node.status === "processing" || node.status === "waiting") {
      nodes[nodeId] = {
        ...node,
        status: "error",
        event: message || "FLOW_ERROR"
      };
    }
  }
  nodes.client = { ...nodes.client, status: "error", event: message || "FLOW_ERROR" };
  nodes.gateway = { ...nodes.gateway, status: "error", event: message || "FLOW_ERROR" };

  const links = Object.fromEntries(
    Object.entries(protocol.links).map(([linkId, status]) => [
      linkId,
      status === "active" ? "error" : status
    ])
  );
  const messages = Object.fromEntries(
    Object.entries(protocol.messages).map(([messageId, status]) => [
      messageId,
      status === "active" ? "error" : status
    ])
  );
  messages.flow_result = "error";

  return { nodes, links, messages };
}

function numberOrNull(value) {
  return Number.isFinite(value) ? value : null;
}
