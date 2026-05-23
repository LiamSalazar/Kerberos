package com.portfolio.auth.core.session;

import java.util.Optional;

public record SessionValidationResult(
        SessionValidationStatus status,
        AuthSession session
) {
    public SessionValidationResult {
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (status == SessionValidationStatus.VALID && session == null) {
            throw new IllegalArgumentException("valid sessions require session details");
        }
    }

    public static SessionValidationResult valid(AuthSession session) {
        return new SessionValidationResult(SessionValidationStatus.VALID, session);
    }

    public static SessionValidationResult invalid(SessionValidationStatus status) {
        if (status == SessionValidationStatus.VALID) {
            throw new IllegalArgumentException("use valid(session) for VALID results");
        }
        return new SessionValidationResult(status, null);
    }

    public boolean valid() {
        return status == SessionValidationStatus.VALID;
    }

    public Optional<AuthSession> sessionOptional() {
        return Optional.ofNullable(session);
    }
}
