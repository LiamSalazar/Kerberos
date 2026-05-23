package com.portfolio.auth.core.session;

import java.time.Instant;
import java.util.Objects;

public record AuthSession(
        String sessionId,
        String requestId,
        String clientId,
        String serviceId,
        Instant issuedAt,
        Instant expiresAt,
        AuthSessionStatus status,
        Instant revokedAt
) {
    public AuthSession {
        requireText(sessionId, "sessionId");
        requireText(requestId, "requestId");
        requireText(clientId, "clientId");
        requireText(serviceId, "serviceId");
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(status, "status");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
        if (status == AuthSessionStatus.ACTIVE && revokedAt != null) {
            throw new IllegalArgumentException("active sessions cannot have revokedAt");
        }
        if (status == AuthSessionStatus.REVOKED && revokedAt == null) {
            throw new IllegalArgumentException("revoked sessions require revokedAt");
        }
    }

    public static AuthSession active(
            String sessionId,
            String requestId,
            String clientId,
            String serviceId,
            Instant issuedAt,
            Instant expiresAt) {
        return new AuthSession(
                sessionId,
                requestId,
                clientId,
                serviceId,
                issuedAt,
                expiresAt,
                AuthSessionStatus.ACTIVE,
                null);
    }

    public AuthSession revoke(Instant revokedAt) {
        return new AuthSession(
                sessionId,
                requestId,
                clientId,
                serviceId,
                issuedAt,
                expiresAt,
                AuthSessionStatus.REVOKED,
                Objects.requireNonNull(revokedAt, "revokedAt"));
    }

    public boolean isExpired(Instant now) {
        Objects.requireNonNull(now, "now");
        return !now.isBefore(expiresAt);
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
