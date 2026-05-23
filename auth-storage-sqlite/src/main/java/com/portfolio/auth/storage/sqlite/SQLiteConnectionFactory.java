package com.portfolio.auth.storage.sqlite;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public final class SQLiteConnectionFactory {
    private final Path databasePath;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("SQLite JDBC driver is not available on the runtime classpath", exception);
        }
    }

    public SQLiteConnectionFactory(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath");
    }

    public Path databasePath() {
        return databasePath;
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
    }
}
