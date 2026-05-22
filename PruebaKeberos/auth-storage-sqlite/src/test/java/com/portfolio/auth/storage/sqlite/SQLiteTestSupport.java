package com.portfolio.auth.storage.sqlite;

import java.nio.file.Path;

final class SQLiteTestSupport {
    private SQLiteTestSupport() {
    }

    static Path migrationsDirectory() {
        return Path.of("..", "scripts", "sqlite", "migrations").normalize();
    }

    static Path initializedDatabase(Path tempDir, String fileName) throws Exception {
        Path database = tempDir.resolve(fileName);
        SQLiteMigrationRunner.applyMigrations(database, migrationsDirectory());
        return database;
    }
}
