package com.portfolio.auth.gateway;

public enum WebSocketMessageType {
    START_AUTH_FLOW,
    VERIFY_SESSION,
    LOGOUT_SESSION,
    PING,
    FLOW_EVENT,
    FLOW_RESULT,
    SESSION_VALID,
    SESSION_INVALID,
    SESSION_LOGGED_OUT,
    ERROR,
    PONG
}
