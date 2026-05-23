package com.portfolio.auth.core.audit;

import java.time.Instant;
import java.util.Objects;

public record AuthAuditEvent(
        String requestId,
        String clientId,
        String serviceId,
        AuthEventType eventType,
        AuthEventStatus status,
        String errorType,
        long latencyMs,
        Instant createdAt
) {
    public AuthAuditEvent {
        requestId = requireText(requestId, "requestId");
        clientId = blankToNull(clientId);
        serviceId = blankToNull(serviceId);
        eventType = Objects.requireNonNull(eventType, "eventType");
        status = Objects.requireNonNull(status, "status");
        errorType = blankToNull(errorType);
        if (latencyMs < 0) {
            throw new IllegalArgumentException("latencyMs must be >= 0");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static AuthAuditEvent started(String requestId, String clientId, String serviceId, Instant createdAt) {
        return new AuthAuditEvent(
                requestId,
                clientId,
                serviceId,
                AuthEventType.AUTH_FLOW_STARTED,
                AuthEventStatus.STARTED,
                null,
                0,
                createdAt);
    }

    public static AuthAuditEvent succeeded(
            String requestId,
            String clientId,
            String serviceId,
            long latencyMs,
            Instant createdAt) {
        return new AuthAuditEvent(
                requestId,
                clientId,
                serviceId,
                AuthEventType.AUTH_FLOW_SUCCEEDED,
                AuthEventStatus.SUCCESS,
                null,
                latencyMs,
                createdAt);
    }

    public static AuthAuditEvent failed(
            String requestId,
            String clientId,
            String serviceId,
            String errorType,
            long latencyMs,
            Instant createdAt) {
        return new AuthAuditEvent(
                requestId,
                clientId,
                serviceId,
                AuthEventType.AUTH_FLOW_FAILED,
                AuthEventStatus.FAILURE,
                errorType,
                latencyMs,
                createdAt);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
