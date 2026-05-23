package com.portfolio.auth.gateway;

public enum WebSocketMessageType {
    START_AUTH_FLOW,
    PING,
    FLOW_EVENT,
    FLOW_RESULT,
    ERROR,
    PONG
}
