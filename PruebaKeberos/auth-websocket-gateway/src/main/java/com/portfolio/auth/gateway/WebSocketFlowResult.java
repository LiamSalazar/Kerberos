package com.portfolio.auth.gateway;

public record WebSocketFlowResult(
        String requestId,
        boolean success,
        String serviceMessage,
        long asMillis,
        long tgsMillis,
        long serviceMillis,
        long totalMillis
) {
    public WebSocketFlowResult {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId es requerido para resultados de flujo");
        }
        if (serviceMessage == null || serviceMessage.isBlank()) {
            throw new IllegalArgumentException("serviceMessage es requerido para resultados de flujo");
        }
        if (asMillis < 0 || tgsMillis < 0 || serviceMillis < 0 || totalMillis < 0) {
            throw new IllegalArgumentException("Las latencias no pueden ser negativas");
        }
    }
}
