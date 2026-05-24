package com.portfolio.auth.storage.postgres;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class PostgresSqlScriptRunner {
    private PostgresSqlScriptRunner() {
    }

    static void runScript(Connection connection, Path script) throws IOException, SQLException {
        String sql = stripLineComments(Files.readString(script, StandardCharsets.UTF_8));
        for (String statementSql : sql.split(";")) {
            String trimmed = statementSql.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(trimmed);
            }
        }
    }

    private static String stripLineComments(String sql) {
        StringBuilder cleaned = new StringBuilder();
        for (String line : sql.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("--")) {
                cleaned.append(line).append(System.lineSeparator());
            }
        }
        return cleaned.toString();
    }
}
