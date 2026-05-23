package com.portfolio.auth.gateway;

import java.time.Instant;

public record WebSocketFlowResult(
        String requestId,
        boolean success,
        WebSocketErrorType errorType,
        String serviceMessage,
        String sessionId,
        Instant sessionExpiresAt,
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
        if (!success && errorType == null) {
            throw new IllegalArgumentException("errorType es requerido cuando success=false");
        }
        if (success && (sessionId == null || sessionId.isBlank() || sessionExpiresAt == null)) {
            throw new IllegalArgumentException("sessionId y sessionExpiresAt son requeridos cuando success=true");
        }
        if (!success && (sessionId != null || sessionExpiresAt != null)) {
            throw new IllegalArgumentException("Las sesiones solo se exponen cuando success=true");
        }
        if (asMillis < 0 || tgsMillis < 0 || serviceMillis < 0 || totalMillis < 0) {
            throw new IllegalArgumentException("Las latencias no pueden ser negativas");
        }
    }
}
