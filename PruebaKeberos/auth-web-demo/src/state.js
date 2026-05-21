export const STAGE_DEFINITIONS = [
  { id: "client", label: "Client" },
  { id: "gateway", label: "Gateway" },
  { id: "as", label: "AS" },
  { id: "tgs", label: "TGS" },
  { id: "service", label: "Service" }
];

export const FLOW_STAGE_TO_STATUS = {
  FLOW_STARTED: {
    client: "running",
    gateway: "running",
    as: "idle",
    tgs: "idle",
    service: "idle"
  },
  AS_REQUEST_SENT: {
    client: "running",
    gateway: "running",
    as: "running",
    tgs: "idle",
    service: "idle"
  },
  AS_RESPONSE_RECEIVED: {
    client: "running",
    gateway: "running",
    as: "success",
    tgs: "idle",
    service: "idle"
  },
  TGS_REQUEST_SENT: {
    client: "running",
    gateway: "running",
    as: "success",
    tgs: "running",
    service: "idle"
  },
  TGS_RESPONSE_RECEIVED: {
    client: "running",
    gateway: "running",
    as: "success",
    tgs: "success",
    service: "idle"
  },
  SERVICE_REQUEST_SENT: {
    client: "running",
    gateway: "running",
    as: "success",
    tgs: "success",
    service: "running"
  },
  SERVICE_RESPONSE_RECEIVED: {
    client: "running",
    gateway: "running",
    as: "success",
    tgs: "success",
    service: "success"
  },
  FLOW_SUCCESS: {
    client: "success",
    gateway: "success",
    as: "success",
    tgs: "success",
    service: "success"
  },
  FLOW_ERROR: {
    client: "error",
    gateway: "error",
    as: "error",
    tgs: "error",
    service: "error"
  }
};

export function createInitialState() {
  return {
    connection: {
      status: "closed",
      label: "Disconnected",
      url: "ws://127.0.0.1:2800"
    },
    inputs: {
      wsUrl: "ws://127.0.0.1:2800",
      clientId: "1",
      serviceId: "1"
    },
    stages: createIdleStages(),
    events: [],
    errors: [],
    result: {
      status: "waiting",
      requestId: "none",
      serviceMessage: "Run a flow to see the protected response.",
      latency: "not available",
      success: null
    },
    activeRequestId: null,
    flowRunning: false
  };
}

export function createIdleStages() {
  return STAGE_DEFINITIONS.reduce((stages, stage) => {
    stages[stage.id] = "idle";
    return stages;
  }, {});
}

export function createRequestId() {
  const suffix = Math.random().toString(16).slice(2, 8);
  return `web-${Date.now()}-${suffix}`;
}

export function applyFlowStage(stages, stageName) {
  const update = FLOW_STAGE_TO_STATUS[stageName];
  if (!update) {
    return stages;
  }
  return { ...stages, ...update };
}

export function formatLatency(message) {
  const parts = [
    ["AS", message.asMillis],
    ["TGS", message.tgsMillis],
    ["Service", message.serviceMillis],
    ["Total", message.totalMillis]
  ]
    .filter(([, value]) => Number.isFinite(value))
    .map(([label, value]) => `${label}: ${value} ms`);

  return parts.length > 0 ? parts.join(" | ") : "not available";
}
