package com.portfolio.auth.storage.postgres;

import com.portfolio.auth.core.repository.PrincipalRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public final class PostgresPrincipalRepository implements PrincipalRepository {
    private static final String TYPE_CLIENT = "CLIENT";
    private static final String TYPE_TGS = "TGS";

    private final PostgresConnectionFactory connectionFactory;

    public PostgresPrincipalRepository(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
    }

    @Override
    public Optional<String> clientSecret(String clientId) {
        return principalSecret(TYPE_CLIENT, clientId);
    }

    @Override
    public Optional<String> ticketGrantingServerSecret(String tgsId) {
        return principalSecret(TYPE_TGS, tgsId);
    }

    private Optional<String> principalSecret(String principalType, String id) {
        String sql = """
                SELECT secret
                FROM principals
                WHERE principal_type = ? AND id = ? AND enabled = TRUE
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, principalType);
            statement.setString(2, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.ofNullable(resultSet.getString("secret"));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read PostgreSQL principal: " + principalType + "/" + id,
                    exception);
        }
    }
}
