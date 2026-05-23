package com.portfolio.auth.gateway;

import com.portfolio.auth.core.session.AuthSession;
import com.portfolio.auth.core.session.SessionValidationStatus;

import java.util.Objects;

public record WebSocketMessage(
        WebSocketMessageType type,
        String requestId,
        String clientId,
        String serviceId,
        String stage,
        String message,
        Boolean success,
        String errorType,
        String serviceMessage,
        String sessionId,
        String sessionExpiresAt,
        Boolean valid,
        String reason,
        String expiresAt,
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
        return new WebSocketMessage(
                type,
                requestId,
                clientId,
                serviceId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public static WebSocketMessage sessionInbound(
            WebSocketMessageType type,
            String requestId,
            String sessionId,
            String clientId,
            String serviceId) {
        return new WebSocketMessage(
                type,
                requestId,
                clientId,
                serviceId,
                null,
                null,
                null,
                null,
                null,
                sessionId,
                null,
                null,
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
                null,
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
                null,
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
            WebSocketErrorType errorType,
            String serviceMessage,
            String sessionId,
            java.time.Instant sessionExpiresAt,
            long asMillis,
            long tgsMillis,
            long serviceMillis,
            long totalMillis) {
        return flowResult(new WebSocketFlowResult(
                requestId,
                success,
                errorType,
                serviceMessage,
                sessionId,
                sessionExpiresAt,
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
                result.errorType() == null ? null : result.errorType().name(),
                result.serviceMessage(),
                result.sessionId(),
                result.sessionExpiresAt() == null ? null : result.sessionExpiresAt().toString(),
                null,
                null,
                null,
                result.asMillis(),
                result.tgsMillis(),
                result.serviceMillis(),
                result.totalMillis());
    }

    public static WebSocketMessage sessionValid(String requestId, AuthSession session) {
        return new WebSocketMessage(
                WebSocketMessageType.SESSION_VALID,
                requestId,
                session.clientId(),
                session.serviceId(),
                null,
                null,
                null,
                null,
                null,
                session.sessionId(),
                null,
                true,
                null,
                session.expiresAt().toString(),
                null,
                null,
                null,
                null);
    }

    public static WebSocketMessage sessionInvalid(String requestId, SessionValidationStatus status) {
        return new WebSocketMessage(
                WebSocketMessageType.SESSION_INVALID,
                requestId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                status.name(),
                null,
                null,
                null,
                null,
                null);
    }

    public static WebSocketMessage sessionLoggedOut(String requestId) {
        return new WebSocketMessage(
                WebSocketMessageType.SESSION_LOGGED_OUT,
                requestId,
                null,
                null,
                null,
                "Sesion revocada",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public static WebSocketMessage error(String requestId, WebSocketErrorType errorType, String message) {
        return error(new WebSocketErrorResponse(requestId, errorType, message));
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
                error.errorType().name(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
