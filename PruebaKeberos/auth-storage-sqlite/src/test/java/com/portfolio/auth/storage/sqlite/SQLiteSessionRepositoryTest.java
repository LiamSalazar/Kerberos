package com.portfolio.auth.storage.sqlite;

import com.portfolio.auth.core.session.AuthSession;
import com.portfolio.auth.core.session.AuthSessionStatus;
import com.portfolio.auth.core.session.SessionValidationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLiteSessionRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldCreateFindValidateAndRevokeSession() throws Exception {
        SQLiteSessionRepository repository = repository("sessions.sqlite");
        Instant now = Instant.parse("2026-05-23T00:00:00Z");
        AuthSession session = AuthSession.active("session-1", "request-1", "client-1", "service-1",
                now, now.plusSeconds(60));

        repository.save(session);

        AuthSession found = repository.findById("session-1").orElseThrow();
        assertEquals("request-1", found.requestId());
        assertEquals(AuthSessionStatus.ACTIVE, found.status());
        assertEquals(SessionValidationStatus.VALID,
                repository.validate("session-1", "client-1", "service-1", now.plusSeconds(1)).status());

        assertTrue(repository.revoke("session-1", now.plusSeconds(2)));
        AuthSession revoked = repository.findById("session-1").orElseThrow();
        assertEquals(AuthSessionStatus.REVOKED, revoked.status());
        assertEquals(SessionValidationStatus.REVOKED,
                repository.validate("session-1", "client-1", "service-1", now.plusSeconds(3)).status());
    }

    @Test
    void shouldRejectExpiredAndMismatchedSessions() throws Exception {
        SQLiteSessionRepository repository = repository("invalid-sessions.sqlite");
        Instant now = Instant.parse("2026-05-23T00:00:00Z");
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

    private SQLiteSessionRepository repository(String fileName) throws Exception {
        Path database = SQLiteTestSupport.initializedDatabase(tempDir, fileName);
        return new SQLiteSessionRepository(database);
    }
}
