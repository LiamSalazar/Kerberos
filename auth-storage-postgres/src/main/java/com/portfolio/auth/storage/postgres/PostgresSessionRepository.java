package com.portfolio.auth.storage.postgres;

import com.portfolio.auth.core.session.AuthSession;
import com.portfolio.auth.core.session.AuthSessionStatus;
import com.portfolio.auth.core.session.SessionRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class PostgresSessionRepository implements SessionRepository {
    private final PostgresConnectionFactory connectionFactory;

    public PostgresSessionRepository(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
    }

    @Override
    public void save(AuthSession session) {
        Objects.requireNonNull(session, "session");
        String sql = """
                INSERT INTO auth_sessions (
                    session_id,
                    request_id,
                    client_id,
                    service_id,
                    issued_at,
                    expires_at,
                    status,
                    revoked_at,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (session_id) DO UPDATE SET
                    request_id = EXCLUDED.request_id,
                    client_id = EXCLUDED.client_id,
                    service_id = EXCLUDED.service_id,
                    issued_at = EXCLUDED.issued_at,
                    expires_at = EXCLUDED.expires_at,
                    status = EXCLUDED.status,
                    revoked_at = EXCLUDED.revoked_at
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, session.sessionId());
            statement.setString(2, session.requestId());
            statement.setString(3, session.clientId());
            statement.setString(4, session.serviceId());
            statement.setTimestamp(5, Timestamp.from(session.issuedAt()));
            statement.setTimestamp(6, Timestamp.from(session.expiresAt()));
            statement.setString(7, session.status().name());
            setTimestamp(statement, 8, session.revokedAt());
            statement.setTimestamp(9, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save PostgreSQL session", exception);
        }
    }

    @Override
    public Optional<AuthSession> findById(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        String sql = """
                SELECT session_id, request_id, client_id, service_id, issued_at, expires_at, status, revoked_at
                FROM auth_sessions
                WHERE session_id = ?
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(session(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read PostgreSQL session", exception);
        }
    }

    @Override
    public boolean revoke(String sessionId, Instant revokedAt) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }
        Objects.requireNonNull(revokedAt, "revokedAt");
        String sql = """
                UPDATE auth_sessions
                SET status = ?, revoked_at = ?
                WHERE session_id = ?
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, AuthSessionStatus.REVOKED.name());
            statement.setTimestamp(2, Timestamp.from(revokedAt));
            statement.setString(3, sessionId);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not revoke PostgreSQL session", exception);
        }
    }

    @Override
    public int cleanupExpired(Instant now) {
        Objects.requireNonNull(now, "now");
        String sql = """
                DELETE FROM auth_sessions
                WHERE expires_at <= ?
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(now));
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not cleanup expired PostgreSQL sessions", exception);
        }
    }

    @Override
    public long activeCount(Instant now) {
        Objects.requireNonNull(now, "now");
        String sql = """
                SELECT COUNT(*) AS active_count
                FROM auth_sessions
                WHERE status = ? AND expires_at > ?
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, AuthSessionStatus.ACTIVE.name());
            statement.setTimestamp(2, Timestamp.from(now));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("active_count") : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not count active PostgreSQL sessions", exception);
        }
    }

    private static AuthSession session(ResultSet resultSet) throws SQLException {
        Timestamp revokedAt = resultSet.getTimestamp("revoked_at");
        return new AuthSession(
                resultSet.getString("session_id"),
                resultSet.getString("request_id"),
                resultSet.getString("client_id"),
                resultSet.getString("service_id"),
                resultSet.getTimestamp("issued_at").toInstant(),
                resultSet.getTimestamp("expires_at").toInstant(),
                AuthSessionStatus.valueOf(resultSet.getString("status")),
                revokedAt == null ? null : revokedAt.toInstant());
    }

    private static void setTimestamp(PreparedStatement statement, int index, Instant value) throws SQLException {
        if (value == null) {
            statement.setTimestamp(index, null);
            return;
        }
        statement.setTimestamp(index, Timestamp.from(value));
    }
}
