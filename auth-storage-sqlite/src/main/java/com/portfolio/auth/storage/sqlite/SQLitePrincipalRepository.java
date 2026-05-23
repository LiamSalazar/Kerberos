package com.portfolio.auth.storage.sqlite;

import com.portfolio.auth.core.repository.PrincipalRepository;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class SQLitePrincipalRepository implements PrincipalRepository {
    private static final String TYPE_CLIENT = "CLIENT";
    private static final String TYPE_TGS = "TGS";

    private final SQLiteConnectionFactory connectionFactory;

    public SQLitePrincipalRepository(Path databasePath) {
        this(new SQLiteConnectionFactory(databasePath));
    }

    public SQLitePrincipalRepository(SQLiteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
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
                WHERE principal_type = ? AND id = ? AND enabled = 1
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
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo leer principal SQLite: " + principalType + "/" + id, e);
        }
    }
}
