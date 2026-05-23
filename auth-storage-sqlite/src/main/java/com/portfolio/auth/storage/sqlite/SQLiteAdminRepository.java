package com.portfolio.auth.storage.sqlite;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SQLiteAdminRepository {
    private static final String TYPE_CLIENT = "CLIENT";

    private final SQLiteConnectionFactory connectionFactory;

    public SQLiteAdminRepository(Path databasePath) {
        this(new SQLiteConnectionFactory(databasePath));
    }

    public SQLiteAdminRepository(SQLiteConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
    }

    public void upsertClient(String id, String displayName, String secret, boolean enabled) {
        requireText(id, "id");
        requireText(displayName, "displayName");
        requireText(secret, "secret");
        String sql = """
                INSERT INTO principals (principal_type, id, display_name, secret, enabled)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(principal_type, id) DO UPDATE SET
                    display_name = excluded.display_name,
                    secret = excluded.secret,
                    enabled = excluded.enabled
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TYPE_CLIENT);
            statement.setString(2, id);
            statement.setString(3, displayName);
            statement.setString(4, secret);
            statement.setInt(5, enabled ? 1 : 0);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not upsert SQLite client: " + id, exception);
        }
    }

    public List<SQLiteClientRecord> listClients() {
        String sql = """
                SELECT id, display_name, enabled
                FROM principals
                WHERE principal_type = ?
                ORDER BY id ASC
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TYPE_CLIENT);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SQLiteClientRecord> clients = new ArrayList<>();
                while (resultSet.next()) {
                    clients.add(new SQLiteClientRecord(
                            resultSet.getString("id"),
                            resultSet.getString("display_name"),
                            resultSet.getInt("enabled") == 1));
                }
                return List.copyOf(clients);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not list SQLite clients", exception);
        }
    }

    public boolean setClientEnabled(String id, boolean enabled) {
        requireText(id, "id");
        String sql = """
                UPDATE principals
                SET enabled = ?
                WHERE principal_type = ? AND id = ?
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, enabled ? 1 : 0);
            statement.setString(2, TYPE_CLIENT);
            statement.setString(3, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update SQLite client: " + id, exception);
        }
    }

    public void upsertService(String id, String displayName, String secret, String endpoint, boolean enabled) {
        requireText(id, "id");
        requireText(displayName, "displayName");
        requireText(secret, "secret");
        requireText(endpoint, "endpoint");
        String sql = """
                INSERT INTO services (id, display_name, secret, endpoint, enabled)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    display_name = excluded.display_name,
                    secret = excluded.secret,
                    endpoint = excluded.endpoint,
                    enabled = excluded.enabled
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, displayName);
            statement.setString(3, secret);
            statement.setString(4, endpoint);
            statement.setInt(5, enabled ? 1 : 0);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not upsert SQLite service: " + id, exception);
        }
    }

    public List<SQLiteServiceRecord> listServices() {
        String sql = """
                SELECT id, display_name, endpoint, enabled
                FROM services
                ORDER BY id ASC
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            List<SQLiteServiceRecord> services = new ArrayList<>();
            while (resultSet.next()) {
                services.add(new SQLiteServiceRecord(
                        resultSet.getString("id"),
                        resultSet.getString("display_name"),
                        resultSet.getString("endpoint"),
                        resultSet.getInt("enabled") == 1));
            }
            return List.copyOf(services);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not list SQLite services", exception);
        }
    }

    public boolean setServiceEnabled(String id, boolean enabled) {
        requireText(id, "id");
        String sql = """
                UPDATE services
                SET enabled = ?
                WHERE id = ?
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, enabled ? 1 : 0);
            statement.setString(2, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update SQLite service: " + id, exception);
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
