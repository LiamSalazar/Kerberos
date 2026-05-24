package com.portfolio.auth.storage.postgres;

import com.portfolio.auth.core.config.AuthConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public final class PostgresConnectionFactory {
    private final String jdbcUrl;
    private final String user;
    private final String password;
    private final String sslMode;

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("PostgreSQL JDBC driver is not available on the runtime classpath",
                    exception);
        }
    }

    public PostgresConnectionFactory(AuthConfig config) {
        this(config.postgresUrl(), config.postgresUser(), config.postgresPassword(), config.postgresSslMode());
    }

    public PostgresConnectionFactory(String jdbcUrl, String user, String password, String sslMode) {
        this.jdbcUrl = requireText(jdbcUrl, "jdbcUrl");
        this.user = requireText(user, "user");
        this.password = password == null ? "" : password;
        this.sslMode = requireText(sslMode, "sslMode");
    }

    public Connection open() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("sslmode", sslMode);
        return DriverManager.getConnection(jdbcUrl, properties);
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    public String user() {
        return user;
    }

    public String sslMode() {
        return sslMode;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
