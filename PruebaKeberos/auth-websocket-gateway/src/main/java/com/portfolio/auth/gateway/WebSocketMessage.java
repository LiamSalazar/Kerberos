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
        return flowEvent(new WebSocketFlowEvent(requestId, stage, message));
    }

    public static WebSocketMessage flowEvent(WebSocketFlowEvent event) {
        return new WebSocketMessage(
                WebSocketMessageType.FLOW_EVENT,
                event.requestId(),
                null,
                null,
                event.stage().name(),
                event.message(),
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
        return flowResult(new WebSocketFlowResult(
                requestId,
                success,
                serviceMessage,
                asMillis,
                tgsMillis,
                serviceMillis,
                totalMillis));
    }

    public static WebSocketMessage flowResult(WebSocketFlowResult result) {
        return new WebSocketMessage(
                WebSocketMessageType.FLOW_RESULT,
                result.requestId(),
                null,
                null,
                null,
                null,
                result.success(),
                result.serviceMessage(),
                result.asMillis(),
                result.tgsMillis(),
                result.serviceMillis(),
                result.totalMillis());
    }

    public static WebSocketMessage error(String requestId, String message) {
        return error(new WebSocketErrorResponse(requestId, message));
    }

    public static WebSocketMessage error(WebSocketErrorResponse error) {
        return new WebSocketMessage(
                WebSocketMessageType.ERROR,
                error.requestId(),
                null,
                null,
                null,
                error.message(),
                false,
                null,
                null,
                null,
                null,
                null);
    }
}
