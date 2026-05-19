package com.portfolio.auth.gateway;

public enum WebSocketMessageType {
    START_AUTH_FLOW,
    RUN_AUDIT_FLOW,
    PING,
    GATEWAY_READY,
    FLOW_EVENT,
    FLOW_RESULT,
    ERROR,
    PONG
}
