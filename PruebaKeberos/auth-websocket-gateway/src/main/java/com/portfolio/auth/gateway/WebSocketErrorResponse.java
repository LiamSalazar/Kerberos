package com.portfolio.auth.gateway;

public record WebSocketErrorResponse(
        String requestId,
        WebSocketErrorType errorType,
        String message
) {
    public WebSocketErrorResponse {
        errorType = java.util.Objects.requireNonNull(errorType, "errorType");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message es requerido para errores WebSocket");
        }
    }
}
