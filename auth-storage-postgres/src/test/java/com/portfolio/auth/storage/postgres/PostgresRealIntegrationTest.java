package com.portfolio.auth.storage.postgres;

import com.portfolio.auth.core.config.AuthConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PostgresRealIntegrationTest {
    @Test
    void shouldApplyMigrationsAgainstExplicitPostgresWhenProfileIsEnabled() throws Exception {
        assumeTrue(Boolean.getBoolean("auth.postgres.it.enabled"),
                "Enable with -Ppostgres-it -Dpostgres.it=true and explicit AUTH_POSTGRES_* values");

        AuthConfig config = AuthConfig.fromEnvironment(Map.ofEntries(
                Map.entry(AuthConfig.ENV_STORAGE_MODE, AuthConfig.STORAGE_MODE_POSTGRES),
                Map.entry(AuthConfig.ENV_POSTGRES_URL, required(AuthConfig.ENV_POSTGRES_URL)),
                Map.entry(AuthConfig.ENV_POSTGRES_USER, required(AuthConfig.ENV_POSTGRES_USER)),
                Map.entry(AuthConfig.ENV_POSTGRES_PASSWORD, required(AuthConfig.ENV_POSTGRES_PASSWORD))));

        PostgresMigrationRunner.applyMigrations(config);
    }

    private static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " is required for postgres-it");
        }
        return value;
    }
}
