package com.portfolio.auth.storage.sqlite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class SqlScriptRunner {
    private SqlScriptRunner() {
    }

    public static void runScripts(Path databasePath, Path... scripts) throws IOException, SQLException {
        SQLiteConnectionFactory connectionFactory = new SQLiteConnectionFactory(databasePath);
        Path parent = databasePath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (Connection connection = connectionFactory.open()) {
            for (Path script : scripts) {
                runScript(connection, script);
            }
        }
    }

    private static void runScript(Connection connection, Path script) throws IOException, SQLException {
        String sql = Files.readString(script, StandardCharsets.UTF_8);
        for (String statementSql : sql.split(";")) {
            String trimmed = statementSql.trim();
            if (trimmed.isBlank() || trimmed.startsWith("--")) {
                continue;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(trimmed);
            }
        }
    }
}
