package com.portfolio.auth.storage.sqlite;

import com.portfolio.auth.core.audit.AuthAuditEvent;
import com.portfolio.auth.core.audit.AuthEventStatus;
import com.portfolio.auth.core.audit.AuthEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SQLiteAuditRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldPersistAndReadSafeAuditEvents() throws Exception {
        Path database = SQLiteTestSupport.initializedDatabase(tempDir, "audit-test.sqlite");
        SQLiteAuditRepository repository = new SQLiteAuditRepository(database);

        repository.append(AuthAuditEvent.started("audit-1", "client-1", "service-1", Instant.parse("2026-05-22T10:00:00Z")));
        repository.append(AuthAuditEvent.failed(
                "audit-1",
                "client-1",
                "service-1",
                "TGS_UNKNOWN_SERVICE",
                12,
                Instant.parse("2026-05-22T10:00:01Z")));

        List<AuthAuditEvent> events = repository.findByRequestId("audit-1");

        assertEquals(2, events.size());
        assertEquals(AuthEventType.AUTH_FLOW_STARTED, events.get(0).eventType());
        assertEquals(AuthEventStatus.STARTED, events.get(0).status());
        assertNull(events.get(0).errorType());
        assertEquals(AuthEventType.AUTH_FLOW_FAILED, events.get(1).eventType());
        assertEquals("TGS_UNKNOWN_SERVICE", events.get(1).errorType());
        assertEquals(12, events.get(1).latencyMs());
        assertEquals(2, repository.findRecent(10).size());
    }
}
