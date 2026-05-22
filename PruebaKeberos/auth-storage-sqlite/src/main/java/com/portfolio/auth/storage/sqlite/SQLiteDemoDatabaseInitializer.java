package com.portfolio.auth.storage.sqlite;

import com.portfolio.auth.core.config.AuthConfig;

import java.nio.file.Path;

public final class SQLiteDemoDatabaseInitializer {
    private SQLiteDemoDatabaseInitializer() {
    }

    public static void main(String[] args) throws Exception {
        Path databasePath = databasePath(args);
        Path schemaPath = Path.of("scripts", "sqlite", "schema.sql");
        Path seedPath = Path.of("scripts", "sqlite", "seed-demo.sql");

        SqlScriptRunner.runScripts(databasePath, schemaPath, seedPath);
        System.out.println("[auth-storage-sqlite] SQLite demo inicializado en " + databasePath.toAbsolutePath());
    }

    private static Path databasePath(String[] args) {
        for (int index = 0; index < args.length; index++) {
            if ("--db".equals(args[index]) && index + 1 < args.length) {
                return Path.of(args[index + 1]);
            }
        }
        String configured = System.getenv(AuthConfig.ENV_SQLITE_PATH);
        if (configured == null || configured.isBlank()) {
            return Path.of(AuthConfig.DEFAULT_LOCAL_SQLITE_PATH);
        }
        return Path.of(configured);
    }
}
