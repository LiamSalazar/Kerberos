package com.portfolio.auth.gateway;

import java.util.Objects;

public record WebSocketMessage(
        WebSocketMessageType type,
        String requestId,
        String clientId,
        String serviceId,
        String stage,
        String message,
        Boolean success,
        String serviceMessage,
        Long asMillis,
        Long tgsMillis,
        Long serviceMillis,
        Long totalMillis
) {
    public WebSocketMessage {
        Objects.requireNonNull(type, "type");
    }

    public static WebSocketMessage inbound(
            WebSocketMessageType type,
            String requestId,
            String clientId,
            String serviceId) {
        return new WebSocketMessage(type, requestId, clientId, serviceId, null, null, null, null, null, null, null, null);
    }

    public static WebSocketMessage ready() {
        return new WebSocketMessage(
                WebSocketMessageType.GATEWAY_READY,
                null,
                null,
                null,
                null,
                "WebSocket Gateway listo",
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public static WebSocketMessage pong(String requestId) {
        return new WebSocketMessage(
                WebSocketMessageType.PONG,
                requestId,
                null,
                null,
                null,
                "pong",
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public static WebSocketMessage flowEvent(String requestId, WebSocketFlowStage stage, String message) {
        return new WebSocketMessage(
                WebSocketMessageType.FLOW_EVENT,
                requestId,
                null,
                null,
                stage.name(),
                message,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public static WebSocketMessage flowResult(
            String requestId,
            boolean success,
            String serviceMessage,
            long asMillis,
            long tgsMillis,
            long serviceMillis,
            long totalMillis) {
        return new WebSocketMessage(
                WebSocketMessageType.FLOW_RESULT,
                requestId,
                null,
                null,
                null,
                null,
                success,
                serviceMessage,
                asMillis,
                tgsMillis,
                serviceMillis,
                totalMillis);
    }

    public static WebSocketMessage error(String requestId, String message) {
        return new WebSocketMessage(
                WebSocketMessageType.ERROR,
                requestId,
                null,
                null,
                null,
                message,
                false,
                null,
                null,
                null,
                null,
                null);
    }
}
