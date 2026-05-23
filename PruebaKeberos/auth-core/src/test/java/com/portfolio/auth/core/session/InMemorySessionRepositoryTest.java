package com.portfolio.auth.core.session;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemorySessionRepositoryTest {

    @Test
    void shouldCreateFindValidateAndRevokeSession() {
        Instant now = Instant.parse("2026-05-23T00:00:00Z");
        InMemorySessionRepository repository = new InMemorySessionRepository();
        AuthSession session = AuthSession.active("session-1", "request-1", "client-1", "service-1",
                now, now.plusSeconds(60));

        repository.save(session);

        assertTrue(repository.findById("session-1").isPresent());
        assertEquals(SessionValidationStatus.VALID,
                repository.validate("session-1", "client-1", "service-1", now.plusSeconds(1)).status());
        assertTrue(repository.revoke("session-1", now.plusSeconds(2)));
        assertEquals(SessionValidationStatus.REVOKED,
                repository.validate("session-1", "client-1", "service-1", now.plusSeconds(3)).status());
    }

    @Test
    void shouldRejectExpiredAndMismatchedSessions() {
        Instant now = Instant.parse("2026-05-23T00:00:00Z");
        InMemorySessionRepository repository = new InMemorySessionRepository();
        repository.save(AuthSession.active("session-1", "request-1", "client-1", "service-1",
                now, now.plusSeconds(60)));

        assertEquals(SessionValidationStatus.EXPIRED,
                repository.validate("session-1", "client-1", "service-1", now.plusSeconds(60)).status());
        assertEquals(SessionValidationStatus.CLIENT_MISMATCH,
                repository.validate("session-1", "other-client", "service-1", now.plusSeconds(1)).status());
        assertEquals(SessionValidationStatus.SERVICE_MISMATCH,
                repository.validate("session-1", "client-1", "other-service", now.plusSeconds(1)).status());
        assertEquals(SessionValidationStatus.NOT_FOUND,
                repository.validate("missing", "client-1", "service-1", now.plusSeconds(1)).status());
        assertFalse(repository.revoke("missing", now));
    }

    @Test
    void shouldGenerateOpaqueUnpredictableSessionIds() {
        SecureSessionIdGenerator generator = new SecureSessionIdGenerator();

        String first = generator.newSessionId();
        String second = generator.newSessionId();

        assertNotEquals(first, second);
        assertTrue(first.length() >= 32);
        assertFalse(first.contains("client"));
        assertFalse(first.contains("ticket"));
    }
}
