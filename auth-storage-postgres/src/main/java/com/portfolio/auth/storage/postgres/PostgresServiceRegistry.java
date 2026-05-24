package com.portfolio.auth.storage.postgres;

import com.portfolio.auth.core.repository.ServiceRegistry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public final class PostgresServiceRegistry implements ServiceRegistry {
    private static final String TYPE_TGS = "TGS";

    private final PostgresConnectionFactory connectionFactory;

    public PostgresServiceRegistry(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
    }

    @Override
    public Optional<String> ticketGrantingServerSecret(String tgsId) {
        String sql = """
                SELECT secret
                FROM principals
                WHERE principal_type = ? AND id = ? AND enabled = TRUE
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
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read PostgreSQL TGS: " + tgsId, exception);
        }
    }

    @Override
    public Optional<String> serviceSecret(String serviceId) {
        String sql = """
                SELECT secret
                FROM services
                WHERE id = ? AND enabled = TRUE
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
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read PostgreSQL service: " + serviceId, exception);
        }
    }
}
