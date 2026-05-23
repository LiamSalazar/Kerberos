package com.portfolio.auth.gateway;

public enum WebSocketErrorType {
    INVALID_JSON,
    UNKNOWN_MESSAGE_TYPE,
    MISSING_REQUIRED_FIELD,
    CLIENT_NOT_FOUND,
    SERVICE_NOT_FOUND,
    FLOW_FAILED,
    RATE_LIMITED,
    ORIGIN_NOT_ALLOWED
}
