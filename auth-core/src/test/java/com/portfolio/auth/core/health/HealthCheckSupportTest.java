package com.portfolio.auth.core.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthCheckSupportTest {
    @Test
    void shouldRenderHealthJsonWithoutSecrets() {
        HealthCheckSupport support = new HealthCheckSupport("auth-websocket-gateway", "test", "postgres");

        String json = support.json();

        assertTrue(json.contains("\"status\":\"UP\""));
        assertTrue(json.contains("\"service\":\"auth-websocket-gateway\""));
        assertTrue(json.contains("\"storageMode\":\"postgres\""));
        assertFalse(json.toLowerCase().contains("password"));
        assertFalse(json.toLowerCase().contains("secret"));
    }
}
