package com.portfolio.auth.storage.sqlite;

import com.portfolio.auth.core.repository.ServiceRegistry;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class SQLiteServiceRegistry implements ServiceRegistry {
    private static final String TYPE_TGS = "TGS";

    private final SQLiteConnectionFactory connectionFactory;

    public SQLiteServiceRegistry(Path databasePath) {
        this(new SQLiteConnectionFactory(databasePath));
    }

    public SQLiteServiceRegistry(SQLiteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Optional<String> ticketGrantingServerSecret(String tgsId) {
        String sql = """
                SELECT secret
                FROM principals
                WHERE principal_type = ? AND id = ? AND enabled = 1
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TYPE_TGS);
            statement.setString(2, tgsId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.ofNullable(resultSet.getString("secret"));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo leer TGS SQLite: " + tgsId, e);
        }
    }

    @Override
    public Optional<String> serviceSecret(String serviceId) {
        String sql = """
                SELECT secret
                FROM services
                WHERE id = ? AND enabled = 1
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serviceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.ofNullable(resultSet.getString("secret"));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo leer servicio SQLite: " + serviceId, e);
        }
    }
}
