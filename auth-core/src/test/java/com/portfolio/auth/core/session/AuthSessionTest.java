package com.portfolio.auth.core.session;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthSessionTest {

    @Test
    void shouldCreateActiveSession() {
        Instant now = Instant.parse("2026-05-23T00:00:00Z");

        AuthSession session = AuthSession.active(
                "opaque-session",
                "request-1",
                "client-1",
                "service-1",
                now,
                now.plusSeconds(60));

        assertEquals(AuthSessionStatus.ACTIVE, session.status());
        assertEquals(null, session.revokedAt());
        assertTrue(session.isExpired(now.plusSeconds(60)));
    }

    @Test
    void shouldRejectInvalidSessionWindow() {
        Instant now = Instant.parse("2026-05-23T00:00:00Z");

        assertThrows(IllegalArgumentException.class,
                () -> AuthSession.active("s", "r", "c", "svc", now, now));
    }

    @Test
    void shouldRevokeSession() {
        Instant now = Instant.parse("2026-05-23T00:00:00Z");
        AuthSession session = AuthSession.active("s", "r", "c", "svc", now, now.plusSeconds(60));

        AuthSession revoked = session.revoke(now.plusSeconds(10));

        assertEquals(AuthSessionStatus.REVOKED, revoked.status());
        assertEquals(now.plusSeconds(10), revoked.revokedAt());
    }
}
