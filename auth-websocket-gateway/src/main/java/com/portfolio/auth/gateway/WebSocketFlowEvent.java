package com.portfolio.auth.gateway;

import java.util.Objects;

public record WebSocketFlowEvent(
        String requestId,
        WebSocketFlowStage stage,
        String message
) {
    public WebSocketFlowEvent {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId es requerido para eventos de flujo");
        }
        Objects.requireNonNull(stage, "stage");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message es requerido para eventos de flujo");
        }
    }
}
