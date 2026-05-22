package com.portfolio.auth.storage.sqlite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SQLiteMigrationRunner {
    private SQLiteMigrationRunner() {
    }

    public static SQLiteMigrationReport applyMigrations(Path databasePath) throws IOException, SQLException {
        return applyMigrations(databasePath, defaultMigrationsDirectory());
    }

    public static SQLiteMigrationReport applyMigrations(Path databasePath, Path migrationsDirectory)
            throws IOException, SQLException {
        Objects.requireNonNull(databasePath, "databasePath");
        Objects.requireNonNull(migrationsDirectory, "migrationsDirectory");

        Path parent = databasePath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        SQLiteConnectionFactory connectionFactory = new SQLiteConnectionFactory(databasePath);
        try (Connection connection = connectionFactory.open()) {
            ensureSchemaVersionTable(connection);
            Set<String> applied = appliedVersions(connection);
            List<MigrationScript> migrations = migrations(migrationsDirectory);
            List<String> appliedNow = new ArrayList<>();
            int skipped = 0;

            for (MigrationScript migration : migrations) {
                if (applied.contains(migration.version())) {
                    skipped++;
                    continue;
                }
                applyMigration(connection, migration);
                appliedNow.add(migration.version());
            }
            return new SQLiteMigrationReport(appliedNow.size(), skipped, appliedNow);
        }
    }

    public static Path defaultMigrationsDirectory() {
        Path direct = Path.of("scripts", "sqlite", "migrations");
        if (Files.isDirectory(direct)) {
            return direct;
        }
        return Path.of("..", "scripts", "sqlite", "migrations").normalize();
    }

    private static void ensureSchemaVersionTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS schema_version (
                    version TEXT PRIMARY KEY,
                    description TEXT NOT NULL,
                    script_name TEXT NOT NULL,
                    checksum TEXT NOT NULL,
                    applied_at TEXT NOT NULL
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static Set<String> appliedVersions(Connection connection) throws SQLException {
        Set<String> applied = new HashSet<>();
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT version FROM schema_version")) {
            while (resultSet.next()) {
                applied.add(resultSet.getString("version"));
            }
        }
        return applied;
    }

    private static List<MigrationScript> migrations(Path migrationsDirectory) throws IOException {
        if (!Files.isDirectory(migrationsDirectory)) {
            throw new IOException("SQLite migrations directory not found: " + migrationsDirectory.toAbsolutePath());
        }

        try (var stream = Files.list(migrationsDirectory)) {
            return stream
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".sql"))
                    .map(SQLiteMigrationRunner::migrationScript)
                    .sorted(Comparator.comparingInt(MigrationScript::numericVersion))
                    .toList();
        }
    }

    private static MigrationScript migrationScript(Path path) {
        String fileName = path.getFileName().toString();
        int separator = fileName.indexOf("__");
        if (!fileName.startsWith("V") || separator < 2) {
            throw new IllegalArgumentException("Invalid SQLite migration name: " + fileName);
        }

        String version = fileName.substring(1, separator);
        int numericVersion;
        try {
            numericVersion = Integer.parseInt(version);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid SQLite migration version: " + fileName, exception);
        }

        String description = fileName.substring(separator + 2, fileName.length() - ".sql".length())
                .replace('_', ' ');
        return new MigrationScript(version, numericVersion, description, fileName, path);
    }

    private static void applyMigration(Connection connection, MigrationScript migration) throws IOException, SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            SqlScriptRunner.runScript(connection, migration.path());
            recordMigration(connection, migration);
            connection.commit();
        } catch (IOException | SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private static void recordMigration(Connection connection, MigrationScript migration) throws IOException, SQLException {
        String sql = """
                INSERT INTO schema_version (version, description, script_name, checksum, applied_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, migration.version());
            statement.setString(2, migration.description());
            statement.setString(3, migration.fileName());
            statement.setString(4, checksum(migration.path()));
            statement.setString(5, Instant.now().toString());
            statement.executeUpdate();
        }
    }

    private static String checksum(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readString(path, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
            byte[] hash = digest.digest(bytes);
            StringBuilder hex = new StringBuilder();
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private record MigrationScript(
            String version,
            int numericVersion,
            String description,
            String fileName,
            Path path
    ) {
    }
}
