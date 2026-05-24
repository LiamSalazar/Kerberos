package com.portfolio.auth.core.session;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public interface SessionRepository {
    void save(AuthSession session);

    Optional<AuthSession> findById(String sessionId);

    boolean revoke(String sessionId, Instant revokedAt);

    default int cleanupExpired(Instant now) {
        Objects.requireNonNull(now, "now");
        return 0;
    }

    default long activeCount(Instant now) {
        Objects.requireNonNull(now, "now");
        return 0;
    }

    default SessionValidationResult validate(
            String sessionId,
            String clientId,
            String serviceId,
            Instant now) {
        if (sessionId == null || sessionId.isBlank()) {
            return SessionValidationResult.invalid(SessionValidationStatus.NOT_FOUND);
        }
        Objects.requireNonNull(now, "now");

        Optional<AuthSession> found = findById(sessionId);
        if (found.isEmpty()) {
            return SessionValidationResult.invalid(SessionValidationStatus.NOT_FOUND);
        }

        AuthSession session = found.get();
        if (session.status() == AuthSessionStatus.REVOKED) {
            return SessionValidationResult.invalid(SessionValidationStatus.REVOKED);
        }
        if (session.isExpired(now)) {
            return SessionValidationResult.invalid(SessionValidationStatus.EXPIRED);
        }
        if (!session.clientId().equals(clientId)) {
            return SessionValidationResult.invalid(SessionValidationStatus.CLIENT_MISMATCH);
        }
        if (!session.serviceId().equals(serviceId)) {
            return SessionValidationResult.invalid(SessionValidationStatus.SERVICE_MISMATCH);
        }
        return SessionValidationResult.valid(session);
    }
}
