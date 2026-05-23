package com.portfolio.auth.storage.sqlite;

import com.portfolio.auth.core.audit.AuthAuditEvent;
import com.portfolio.auth.core.audit.AuthEventRepository;
import com.portfolio.auth.core.audit.AuthEventStatus;
import com.portfolio.auth.core.audit.AuthEventType;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SQLiteAuditRepository implements AuthEventRepository {
    private final SQLiteConnectionFactory connectionFactory;

    public SQLiteAuditRepository(Path databasePath) {
        this(new SQLiteConnectionFactory(databasePath));
    }

    public SQLiteAuditRepository(SQLiteConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
    }

    @Override
    public void append(AuthAuditEvent event) {
        Objects.requireNonNull(event, "event");
        String sql = """
                INSERT INTO auth_audit_events (
                    request_id,
                    client_id,
                    service_id,
                    event_type,
                    status,
                    error_type,
                    latency_ms,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, event.requestId());
            statement.setString(2, event.clientId());
            statement.setString(3, event.serviceId());
            statement.setString(4, event.eventType().name());
            statement.setString(5, event.status().name());
            statement.setString(6, event.errorType());
            statement.setLong(7, event.latencyMs());
            statement.setString(8, event.createdAt().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not append SQLite audit event for requestId=" + event.requestId(),
                    exception);
        }
    }

    @Override
    public List<AuthAuditEvent> findRecent(int limit) {
        if (limit < 1) {
            return List.of();
        }
        String sql = """
                SELECT request_id, client_id, service_id, event_type, status, error_type, latency_ms, created_at
                FROM auth_audit_events
                ORDER BY id DESC
                LIMIT ?
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                return events(resultSet);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read recent SQLite audit events", exception);
        }
    }

    @Override
    public List<AuthAuditEvent> findByRequestId(String requestId) {
        return findByField("request_id", requestId, "requestId");
    }

    @Override
    public List<AuthAuditEvent> findByClientId(String clientId) {
        return findByField("client_id", clientId, "clientId");
    }

    @Override
    public List<AuthAuditEvent> findByServiceId(String serviceId) {
        return findByField("service_id", serviceId, "serviceId");
    }

    private List<AuthAuditEvent> findByField(String column, String value, String label) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String sql = """
                SELECT request_id, client_id, service_id, event_type, status, error_type, latency_ms, created_at
                FROM auth_audit_events
                WHERE %s = ?
                ORDER BY id ASC
                """.formatted(column);
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return events(resultSet);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read SQLite audit events for " + label + "=" + value, exception);
        }
    }

    private static List<AuthAuditEvent> events(ResultSet resultSet) throws SQLException {
        List<AuthAuditEvent> events = new ArrayList<>();
        while (resultSet.next()) {
            events.add(new AuthAuditEvent(
                    resultSet.getString("request_id"),
                    resultSet.getString("client_id"),
                    resultSet.getString("service_id"),
                    AuthEventType.valueOf(resultSet.getString("event_type")),
                    AuthEventStatus.valueOf(resultSet.getString("status")),
                    resultSet.getString("error_type"),
                    resultSet.getLong("latency_ms"),
                    Instant.parse(resultSet.getString("created_at"))));
        }
        return List.copyOf(events);
    }
}
