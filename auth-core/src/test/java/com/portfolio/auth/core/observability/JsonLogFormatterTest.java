package com.portfolio.auth.core.observability;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonLogFormatterTest {
    @Test
    void shouldRedactSensitiveFields() {
        String log = JsonLogFormatter.format(Map.of(
                "service", "auth-websocket-gateway",
                "postgresPassword", "super-secret",
                "sessionId", "full-session-id",
                "event", "test"));

        assertTrue(log.contains("\"postgresPassword\":\"<redacted>\""));
        assertTrue(log.contains("\"sessionId\":\"<redacted>\""));
        assertFalse(log.contains("super-secret"));
        assertFalse(log.contains("full-session-id"));
    }
}
