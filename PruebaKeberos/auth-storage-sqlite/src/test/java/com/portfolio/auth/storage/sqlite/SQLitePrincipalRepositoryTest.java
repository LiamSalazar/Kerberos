package com.portfolio.auth.storage.sqlite;

import com.portfolio.auth.core.config.AuthConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLitePrincipalRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldReadEnabledClientAndTgsSecrets() throws Exception {
        Path database = initializedDatabase();
        SQLitePrincipalRepository repository = new SQLitePrincipalRepository(database);

        assertEquals(
                AuthConfig.DEFAULT_LOCAL_DEMO_CLIENT_SECRET,
                repository.clientSecret(AuthConfig.DEFAULT_LOCAL_CLIENT_ID).orElseThrow());
        assertEquals(
                AuthConfig.DEFAULT_LOCAL_DEMO_TGS_SECRET,
                repository.ticketGrantingServerSecret(AuthConfig.DEFAULT_LOCAL_TGS_ID).orElseThrow());
        assertTrue(repository.clientSecret("missing").isEmpty());
    }

    private Path initializedDatabase() throws Exception {
        Path database = tempDir.resolve("auth-test.sqlite");
        SqlScriptRunner.runScripts(
                database,
                Path.of("..", "scripts", "sqlite", "schema.sql").normalize(),
                Path.of("..", "scripts", "sqlite", "seed-demo.sql").normalize());
        return database;
    }
}
