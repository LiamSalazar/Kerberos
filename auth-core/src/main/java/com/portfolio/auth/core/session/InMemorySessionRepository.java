package com.portfolio.auth.core.session;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemorySessionRepository implements SessionRepository {
    private final ConcurrentMap<String, AuthSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(AuthSession session) {
        Objects.requireNonNull(session, "session");
        sessions.put(session.sessionId(), session);
    }

    @Override
    public Optional<AuthSession> findById(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public boolean revoke(String sessionId, Instant revokedAt) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }
        Objects.requireNonNull(revokedAt, "revokedAt");
        return sessions.computeIfPresent(sessionId, (ignored, session) -> session.revoke(revokedAt)) != null;
    }

    @Override
    public int cleanupExpired(Instant now) {
        Objects.requireNonNull(now, "now");
        int before = sessions.size();
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        return before - sessions.size();
    }

    @Override
    public long activeCount(Instant now) {
        Objects.requireNonNull(now, "now");
        return sessions.values().stream()
                .filter(session -> session.status() == AuthSessionStatus.ACTIVE)
                .filter(session -> !session.isExpired(now))
                .count();
    }
}
