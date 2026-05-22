package com.portfolio.auth.storage.sqlite;

import com.portfolio.auth.core.config.AuthConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLiteServiceRegistryTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldReadEnabledTgsAndServiceSecrets() throws Exception {
        Path database = initializedDatabase();
        SQLiteServiceRegistry registry = new SQLiteServiceRegistry(database);

        assertEquals(
                AuthConfig.DEFAULT_LOCAL_DEMO_TGS_SECRET,
                registry.ticketGrantingServerSecret(AuthConfig.DEFAULT_LOCAL_TGS_ID).orElseThrow());
        assertEquals(
                AuthConfig.DEFAULT_LOCAL_DEMO_SERVICE_SECRET,
                registry.serviceSecret(AuthConfig.DEFAULT_LOCAL_SERVICE_ID).orElseThrow());
        assertTrue(registry.serviceSecret("missing").isEmpty());
    }

    private Path initializedDatabase() throws Exception {
        return SQLiteTestSupport.initializedDatabase(tempDir, "auth-test.sqlite");
    }
}
