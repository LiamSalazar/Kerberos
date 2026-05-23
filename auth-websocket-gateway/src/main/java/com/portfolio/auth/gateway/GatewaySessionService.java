package com.portfolio.auth.gateway;

import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.session.AuthSession;
import com.portfolio.auth.core.session.InMemorySessionRepository;
import com.portfolio.auth.core.session.SecureSessionIdGenerator;
import com.portfolio.auth.core.session.SessionRepository;
import com.portfolio.auth.core.session.SessionValidationResult;
import com.portfolio.auth.core.session.SessionValidationStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

public final class GatewaySessionService {
    private final SessionRepository repository;
    private final Duration sessionTtl;
    private final Duration sessionMaxTtl;
    private final Clock clock;
    private final Supplier<String> sessionIds;

    public GatewaySessionService(AuthConfig config, SessionRepository repository) {
        this(
                repository,
                config.sessionTtl(),
                config.sessionMaxTtl(),
                Clock.systemUTC(),
                new SecureSessionIdGenerator()::newSessionId);
    }

    GatewaySessionService(
            SessionRepository repository,
            Duration sessionTtl,
            Duration sessionMaxTtl,
            Clock clock,
            Supplier<String> sessionIds) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.sessionTtl = requirePositive(sessionTtl, "sessionTtl");
        this.sessionMaxTtl = requirePositive(sessionMaxTtl, "sessionMaxTtl");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sessionIds = Objects.requireNonNull(sessionIds, "sessionIds");
    }

    static GatewaySessionService inMemory(AuthConfig config) {
        return new GatewaySessionService(config, new InMemorySessionRepository());
    }

    public AuthSession createSession(
            String requestId,
            String clientId,
            String serviceId,
            Instant serviceExpiresAt) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = earliest(issuedAt.plus(sessionTtl), issuedAt.plus(sessionMaxTtl));
        if (serviceExpiresAt != null && serviceExpiresAt.isAfter(issuedAt)) {
            expiresAt = earliest(expiresAt, serviceExpiresAt);
        }

        AuthSession session = AuthSession.active(
                sessionIds.get(),
                requestId,
                clientId,
                serviceId,
                issuedAt,
                expiresAt);
        repository.save(session);
        return session;
    }

    public WebSocketMessage verify(WebSocketMessage input) {
        SessionValidationResult result = repository.validate(
                input.sessionId(),
                input.clientId(),
                input.serviceId(),
                Instant.now(clock));
        if (result.valid()) {
            return WebSocketMessage.sessionValid(input.requestId(), result.session());
        }
        return WebSocketMessage.sessionInvalid(input.requestId(), result.status());
    }

    public WebSocketMessage logout(WebSocketMessage input) {
        boolean revoked = repository.revoke(input.sessionId(), Instant.now(clock));
        if (!revoked) {
            return WebSocketMessage.error(
                    input.requestId(),
                    WebSocketErrorType.SESSION_NOT_FOUND,
                    "Sesion no encontrada");
        }
        return WebSocketMessage.sessionLoggedOut(input.requestId());
    }

    private static Instant earliest(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private static Duration requirePositive(Duration duration, String fieldName) {
        Objects.requireNonNull(duration, fieldName);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return duration;
    }
}
