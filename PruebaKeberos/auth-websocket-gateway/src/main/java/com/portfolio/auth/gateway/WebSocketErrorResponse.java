package com.portfolio.auth.gateway;

public record WebSocketErrorResponse(
        String requestId,
        String message
) {
    public WebSocketErrorResponse {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message es requerido para errores WebSocket");
        }
    }
}
