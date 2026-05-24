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
        assertEquals(AuthConfig.STORAGE_MODE_MEMORY, config.storageMode());
        assertEquals(AuthConfig.DEFAULT_LOCAL_SQLITE_PATH, config.sqlitePath());
        assertEquals(AuthConfig.DEFAULT_LOCAL_POSTGRES_URL, config.postgresUrl());
        assertEquals(AuthConfig.DEFAULT_LOCAL_POSTGRES_USER, config.postgresUser());
        assertEquals(AuthConfig.DEFAULT_LOCAL_POSTGRES_SSL_MODE, config.postgresSslMode());
        assertEquals(AuthConfig.DEFAULT_LOCAL_SESSION_TTL, config.sessionTtl());
        assertEquals(AuthConfig.DEFAULT_LOCAL_SESSION_MAX_TTL, config.sessionMaxTtl());
        assertEquals(AuthConfig.STORAGE_MODE_MEMORY, config.sessionStorageMode());
        assertEquals(AuthConfig.SECRET_PROVIDER_ENV, config.secretProvider());
        assertTrue(config.requireSessionVerify());
        assertTrue(config.usesDemoSecrets());
        assertTrue(AuthConfig.demoWarning(Map.of(), config).contains(AuthConfig.ENV_AUTH_MODE));
    }

    @Test
    void shouldOverrideBasicRuntimeValuesFromEnvironment() {
        AuthConfig config = AuthConfig.fromEnvironment(Map.ofEntries(
                Map.entry(AuthConfig.ENV_AS_PORT, "2100"),
                Map.entry(AuthConfig.ENV_TGS_PORT, "2101"),
                Map.entry(AuthConfig.ENV_SERVICE_PORT, "2102"),
                Map.entry(AuthConfig.ENV_TICKET_TTL_MINUTES, "9"),
                Map.entry(AuthConfig.ENV_ALLOWED_SKEW_SECONDS, "30"),
                Map.entry(AuthConfig.ENV_REPLAY_WINDOW_SECONDS, "45"),
                Map.entry(AuthConfig.ENV_STORAGE_MODE, AuthConfig.STORAGE_MODE_SQLITE),
                Map.entry(AuthConfig.ENV_SQLITE_PATH, "target/test-auth.sqlite"),
                Map.entry(AuthConfig.ENV_POSTGRES_URL, "jdbc:postgresql://db.internal:5432/auth"),
                Map.entry(AuthConfig.ENV_POSTGRES_USER, "auth_user"),
                Map.entry(AuthConfig.ENV_POSTGRES_PASSWORD, "postgres-secret"),
                Map.entry(AuthConfig.ENV_POSTGRES_SSL_MODE, "require"),
                Map.entry(AuthConfig.ENV_SESSION_TTL_SECONDS, "120"),
                Map.entry(AuthConfig.ENV_SESSION_MAX_TTL_SECONDS, "180"),
                Map.entry(AuthConfig.ENV_REQUIRE_SESSION_VERIFY, "false")));

        assertEquals(2100, config.authenticationServerPort());
        assertEquals(2101, config.ticketGrantingServerPort());
        assertEquals(2102, config.serviceServerPort());
        assertEquals(Duration.ofMinutes(9), config.ticketLifetime());
        assertEquals(Duration.ofSeconds(30), config.allowedClockSkew());
        assertEquals(Duration.ofSeconds(45), config.replayWindow());
        assertEquals(AuthConfig.STORAGE_MODE_SQLITE, config.storageMode());
        assertEquals("target/test-auth.sqlite", config.sqlitePath());
        assertEquals("jdbc:postgresql://db.internal:5432/auth", config.postgresUrl());
        assertEquals("auth_user", config.postgresUser());
        assertEquals("postgres-secret", config.postgresPassword());
        assertEquals("require", config.postgresSslMode());
        assertTrue(config.usesSqliteStorage());
        assertEquals(Duration.ofSeconds(120), config.sessionTtl());
        assertEquals(Duration.ofSeconds(180), config.sessionMaxTtl());
        assertEquals(AuthConfig.STORAGE_MODE_SQLITE, config.sessionStorageMode());
        assertTrue(config.usesSqliteSessionStorage());
        assertFalse(config.requireSessionVerify());
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
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> AuthConfig.fromEnvironment(Map.of(AuthConfig.ENV_AUTH_MODE, AuthConfig.MODE_STRICT)));

        assertTrue(error.getMessage().contains(AuthConfig.ENV_DEMO_CLIENT_SECRET));
    }

    @Test
    void shouldAcceptExplicitSecretsInStrictMode() {
        AuthConfig config = AuthConfig.fromEnvironment(Map.ofEntries(
                Map.entry(AuthConfig.ENV_AUTH_MODE, AuthConfig.MODE_STRICT),
                Map.entry(AuthConfig.ENV_DEMO_CLIENT_SECRET, "client-secret-from-env"),
                Map.entry(AuthConfig.ENV_DEMO_TGS_SECRET, "tgs-secret-from-env"),
                Map.entry(AuthConfig.ENV_DEMO_SERVICE_SECRET, "service-secret-from-env")));

        assertEquals("client-secret-from-env", config.demoClientSecret());
        assertEquals("tgs-secret-from-env", config.demoTicketGrantingServerSecret());
        assertEquals("service-secret-from-env", config.demoServiceSecret());
        assertFalse(config.usesDemoSecrets());
        assertEquals("", AuthConfig.demoWarning(Map.of(AuthConfig.ENV_AUTH_MODE, AuthConfig.MODE_STRICT), config));
        assertTrue(config.requireSessionVerify());
    }

    @Test
    void shouldRejectUnknownExecutionMode() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AuthConfig.fromEnvironment(Map.of(AuthConfig.ENV_AUTH_MODE, "prod")));

        assertTrue(error.getMessage().contains(AuthConfig.ENV_AUTH_MODE));
    }

    @Test
    void shouldRejectUnknownStorageMode() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AuthConfig.fromEnvironment(Map.of(AuthConfig.ENV_STORAGE_MODE, "oracle")));

        assertTrue(error.getMessage().contains(AuthConfig.ENV_STORAGE_MODE));
    }

    @Test
    void shouldRejectUnknownSessionStorageMode() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AuthConfig.fromEnvironment(Map.of(AuthConfig.ENV_SESSION_STORAGE_MODE, "redis")));

        assertTrue(error.getMessage().contains(AuthConfig.ENV_SESSION_STORAGE_MODE));
    }

    @Test
    void shouldAcceptPostgresStorageAndSessionFallback() {
        AuthConfig config = AuthConfig.fromEnvironment(Map.ofEntries(
                Map.entry(AuthConfig.ENV_STORAGE_MODE, AuthConfig.STORAGE_MODE_POSTGRES),
                Map.entry(AuthConfig.ENV_POSTGRES_URL, "jdbc:postgresql://localhost:5432/auth"),
                Map.entry(AuthConfig.ENV_POSTGRES_USER, "auth_user"),
                Map.entry(AuthConfig.ENV_POSTGRES_PASSWORD, "auth_password")));

        assertTrue(config.usesPostgresStorage());
        assertTrue(config.usesPostgresSessionStorage());
        assertEquals(AuthConfig.STORAGE_MODE_POSTGRES, config.sessionStorageMode());
    }

    @Test
    void shouldResolveStrictSecretsFromEnvSecretReferences() {
        AuthConfig config = AuthConfig.fromEnvironment(Map.ofEntries(
                Map.entry(AuthConfig.ENV_AUTH_MODE, AuthConfig.MODE_STRICT),
                Map.entry(AuthConfig.ENV_SECRET_CLIENT_SECRET_ID, "CLIENT_SECRET_ENV"),
                Map.entry(AuthConfig.ENV_SECRET_TGS_SECRET_ID, "TGS_SECRET_ENV"),
                Map.entry(AuthConfig.ENV_SECRET_SERVICE_SECRET_ID, "SERVICE_SECRET_ENV"),
                Map.entry("CLIENT_SECRET_ENV", "client-secret-from-ref"),
                Map.entry("TGS_SECRET_ENV", "tgs-secret-from-ref"),
                Map.entry("SERVICE_SECRET_ENV", "service-secret-from-ref")));

        assertEquals("client-secret-from-ref", config.demoClientSecret());
        assertEquals("tgs-secret-from-ref", config.demoTicketGrantingServerSecret());
        assertEquals("service-secret-from-ref", config.demoServiceSecret());
        assertFalse(config.usesDemoSecrets());
    }

    @Test
    void shouldResolveAwsSecretsWithInjectedProviderWithoutCallingAws() {
        AuthConfig config = AuthConfig.fromEnvironment(
                Map.ofEntries(
                        Map.entry(AuthConfig.ENV_AUTH_MODE, AuthConfig.MODE_STRICT),
                        Map.entry(AuthConfig.ENV_SECRET_PROVIDER, AuthConfig.SECRET_PROVIDER_AWS_SECRETS_MANAGER),
                        Map.entry(AuthConfig.ENV_SECRET_CLIENT_SECRET_ID, "client-secret-id"),
                        Map.entry(AuthConfig.ENV_SECRET_TGS_SECRET_ID, "tgs-secret-id"),
                        Map.entry(AuthConfig.ENV_SECRET_SERVICE_SECRET_ID, "service-secret-id")),
                secretRef -> Map.of(
                                "client-secret-id", "client-secret-from-aws",
                                "tgs-secret-id", "tgs-secret-from-aws",
                                "service-secret-id", "service-secret-from-aws")
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().equals(secretRef.id()))
                        .map(Map.Entry::getValue)
                        .findFirst());

        assertEquals(AuthConfig.SECRET_PROVIDER_AWS_SECRETS_MANAGER, config.secretProvider());
        assertEquals("client-secret-from-aws", config.demoClientSecret());
        assertEquals("tgs-secret-from-aws", config.demoTicketGrantingServerSecret());
        assertEquals("service-secret-from-aws", config.demoServiceSecret());
    }
}
