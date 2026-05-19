package com.portfolio.auth.core.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthConfigTest {

    @Test
    void shouldExposeLocalDemoDefaultsAsDemoOnlyValues() {
        AuthConfig config = AuthConfig.localDemo();

        assertEquals(AuthConfig.DEFAULT_LOCAL_CLIENT_ID, config.defaultClientId());
        assertEquals(AuthConfig.DEFAULT_LOCAL_HOST, config.authenticationServerHost());
        assertEquals(AuthConfig.DEFAULT_LOCAL_AS_PORT, config.authenticationServerPort());
        assertEquals(AuthConfig.DEFAULT_LOCAL_TICKET_LIFETIME, config.ticketLifetime());
        assertEquals(AuthConfig.DEFAULT_LOCAL_DEMO_CLIENT_SECRET, config.demoClientSecret());
        assertTrue(config.usesDemoSecrets());
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

    @Test
    void shouldRejectDemoSecretsInStrictMode() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> AuthConfig.fromEnvironment(Map.of(AuthConfig.ENV_AUTH_MODE, AuthConfig.MODE_STRICT)));

        assertTrue(error.getMessage().contains(AuthConfig.ENV_DEMO_CLIENT_SECRET));
        assertTrue(error.getMessage().contains(AuthConfig.ENV_DEMO_SERVICE_SECRET));
    }

    @Test
    void shouldAcceptExplicitSecretsInStrictMode() {
        AuthConfig config = AuthConfig.fromEnvironment(Map.ofEntries(
                Map.entry(AuthConfig.ENV_AUTH_MODE, AuthConfig.MODE_STRICT),
                Map.entry(AuthConfig.ENV_DEMO_CLIENT_SECRET, "client-secret-from-env"),
                Map.entry(AuthConfig.ENV_DEMO_CLIENT_TGS_KEY, "client-tgs-key-from-env"),
                Map.entry(AuthConfig.ENV_DEMO_TGS_SECRET, "tgs-secret-from-env"),
                Map.entry(AuthConfig.ENV_DEMO_CLIENT_SERVICE_KEY, "client-service-key-from-env"),
                Map.entry(AuthConfig.ENV_DEMO_SERVICE_SECRET, "service-secret-from-env"),
                Map.entry(AuthConfig.ENV_DEMO_PBKDF2_SALT, "salt-from-env")));

        assertEquals("client-secret-from-env", config.demoClientSecret());
        assertEquals("service-secret-from-env", config.demoServiceSecret());
        assertFalse(config.usesDemoSecrets());
    }

    @Test
    void shouldAcceptTemporaryLegacyAliasesForStrictMode() {
        AuthConfig config = AuthConfig.fromEnvironment(Map.ofEntries(
                Map.entry(AuthConfig.ENV_AUTH_MODE, AuthConfig.MODE_STRICT),
                Map.entry(AuthConfig.ENV_ALIAS_LEGACY_CLIENT_SECRET, "client-secret-from-alias"),
                Map.entry(AuthConfig.ENV_ALIAS_LEGACY_CLIENT_TGS_KEY, "client-tgs-key-from-alias"),
                Map.entry(AuthConfig.ENV_ALIAS_LEGACY_TGS_SECRET, "tgs-secret-from-alias"),
                Map.entry(AuthConfig.ENV_ALIAS_LEGACY_CLIENT_SERVICE_KEY, "client-service-key-from-alias"),
                Map.entry(AuthConfig.ENV_ALIAS_LEGACY_SERVICE_SECRET, "service-secret-from-alias"),
                Map.entry(AuthConfig.ENV_ALIAS_LEGACY_PBKDF2_SALT, "salt-from-alias")));

        assertEquals("client-secret-from-alias", config.demoClientSecret());
        assertEquals("service-secret-from-alias", config.demoServiceSecret());
        assertFalse(config.usesDemoSecrets());
    }

    @Test
    void shouldRejectUnknownExecutionMode() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AuthConfig.fromEnvironment(Map.of(AuthConfig.ENV_AUTH_MODE, "prod")));

        assertTrue(error.getMessage().contains(AuthConfig.ENV_AUTH_MODE));
    }
}
