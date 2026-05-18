package com.portfolio.auth.core.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthConfigTest {

    @Test
    void shouldExposeLocalDemoDefaultsAsDemoOnlyValues() {
        AuthConfig config = AuthConfig.localDemo();

        assertEquals(AuthConfig.DEFAULT_LOCAL_CLIENT_ID, config.defaultClientId());
        assertEquals(AuthConfig.DEFAULT_LOCAL_HOST, config.authenticationServerHost());
        assertEquals(AuthConfig.DEFAULT_LOCAL_AS_PORT, config.authenticationServerPort());
        assertEquals(AuthConfig.DEFAULT_LOCAL_TICKET_LIFETIME, config.ticketLifetime());
        assertEquals(AuthConfig.DEFAULT_LOCAL_LEGACY_CLIENT_SECRET, config.legacyClientSecret());
    }

    @Test
    void shouldOverrideBasicRuntimeValuesFromEnvironment() {
        AuthConfig config = AuthConfig.fromEnvironment(Map.of(
                AuthConfig.ENV_AS_PORT, "2100",
                AuthConfig.ENV_TGS_PORT, "2101",
                AuthConfig.ENV_SERVICE_PORT, "2102",
                AuthConfig.ENV_TICKET_TTL_MINUTES, "9",
                AuthConfig.ENV_ALLOWED_SKEW_SECONDS, "30",
                AuthConfig.ENV_REPLAY_WINDOW_SECONDS, "45"));

        assertEquals(2100, config.authenticationServerPort());
        assertEquals(2101, config.ticketGrantingServerPort());
        assertEquals(2102, config.serviceServerPort());
        assertEquals(Duration.ofMinutes(9), config.ticketLifetime());
        assertEquals(Duration.ofSeconds(30), config.allowedClockSkew());
        assertEquals(Duration.ofSeconds(45), config.replayWindow());
    }

    @Test
    void shouldKeepDefaultsWhenNumericEnvironmentValuesAreInvalid() {
        AuthConfig config = AuthConfig.fromEnvironment(Map.of(
                AuthConfig.ENV_AS_PORT, "not-a-port",
                AuthConfig.ENV_TICKET_TTL_MINUTES, "not-a-duration"));

        assertEquals(AuthConfig.DEFAULT_LOCAL_AS_PORT, config.authenticationServerPort());
        assertEquals(AuthConfig.DEFAULT_LOCAL_TICKET_LIFETIME, config.ticketLifetime());
    }
}
