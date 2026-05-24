package com.portfolio.auth.storage.postgres;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresMigrationScriptsTest {
    @Test
    void shouldExposeExpectedVersionedMigrationScripts() throws IOException {
        Path migrations = PostgresMigrationRunner.defaultMigrationsDirectory();

        List<String> names;
        try (var stream = Files.list(migrations)) {
            names = stream
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".sql"))
                    .sorted()
                    .toList();
        }

        assertEquals(List.of(
                "V1__schema.sql",
                "V2__seed_demo.sql",
                "V3__audit_events.sql",
                "V4__audit_query_indexes.sql",
                "V5__auth_sessions.sql"), names);
    }

    @Test
    void shouldKeepSchemaConceptuallyCompatibleWithSqlite() throws IOException {
        Path migrations = PostgresMigrationRunner.defaultMigrationsDirectory();
        String combined = Files.readString(migrations.resolve("V1__schema.sql"))
                + Files.readString(migrations.resolve("V3__audit_events.sql"))
                + Files.readString(migrations.resolve("V5__auth_sessions.sql"));

        assertTrue(combined.contains("principals"));
        assertTrue(combined.contains("services"));
        assertTrue(combined.contains("auth_audit_events"));
        assertTrue(combined.contains("auth_sessions"));
    }
}
