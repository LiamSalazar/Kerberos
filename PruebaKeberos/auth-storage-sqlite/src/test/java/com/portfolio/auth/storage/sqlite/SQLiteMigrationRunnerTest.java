package com.portfolio.auth.storage.sqlite;

import com.portfolio.auth.core.config.AuthConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SQLiteMigrationRunnerTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldApplyMigrationsOnceAndSeedDemoData() throws Exception {
        Path database = tempDir.resolve("migration-test.sqlite");

        SQLiteMigrationReport first = SQLiteMigrationRunner.applyMigrations(
                database,
                SQLiteTestSupport.migrationsDirectory());
        SQLiteMigrationReport second = SQLiteMigrationRunner.applyMigrations(
                database,
                SQLiteTestSupport.migrationsDirectory());

        assertEquals(4, first.appliedCount());
        assertEquals(0, first.skippedCount());
        assertEquals(0, second.appliedCount());
        assertEquals(4, second.skippedCount());

        SQLiteConnectionFactory connectionFactory = new SQLiteConnectionFactory(database);
        try (Connection connection = connectionFactory.open()) {
            assertEquals(4, count(connection, "schema_version"));
            assertEquals(
                    AuthConfig.DEFAULT_LOCAL_DEMO_CLIENT_SECRET,
                    scalar(connection,
                            "SELECT secret FROM principals WHERE principal_type = 'CLIENT' AND id = '1'"));
            assertEquals(
                    AuthConfig.DEFAULT_LOCAL_DEMO_SERVICE_SECRET,
                    scalar(connection, "SELECT secret FROM services WHERE id = '1'"));
        }
    }

    private static int count(Connection connection, String table) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
                ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private static String scalar(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getString(1) : null;
        }
    }
}
