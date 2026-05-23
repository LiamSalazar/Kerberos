package com.portfolio.auth.storage.sqlite;

import com.portfolio.auth.core.session.AuthSession;
import com.portfolio.auth.core.session.AuthSessionStatus;
import com.portfolio.auth.core.session.SessionRepository;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class SQLiteSessionRepository implements SessionRepository {
    private final SQLiteConnectionFactory connectionFactory;

    public SQLiteSessionRepository(Path databasePath) {
        this(new SQLiteConnectionFactory(databasePath));
    }

    public SQLiteSessionRepository(SQLiteConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
    }

    @Override
    public void save(AuthSession session) {
        Objects.requireNonNull(session, "session");
        String sql = """
                INSERT OR REPLACE INTO auth_sessions (
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
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, COALESCE(
                    (SELECT created_at FROM auth_sessions WHERE session_id = ?),
                    ?
                ))
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, session.sessionId());
            statement.setString(2, session.requestId());
            statement.setString(3, session.clientId());
            statement.setString(4, session.serviceId());
            statement.setString(5, session.issuedAt().toString());
            statement.setString(6, session.expiresAt().toString());
            statement.setString(7, session.status().name());
            statement.setString(8, session.revokedAt() == null ? null : session.revokedAt().toString());
            statement.setString(9, session.sessionId());
            statement.setString(10, Instant.now().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save SQLite session " + session.sessionId(), exception);
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
            throw new IllegalStateException("Could not read SQLite session " + sessionId, exception);
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
            statement.setString(2, revokedAt.toString());
            statement.setString(3, sessionId);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not revoke SQLite session " + sessionId, exception);
        }
    }

    private static AuthSession session(ResultSet resultSet) throws SQLException {
        String revokedAt = resultSet.getString("revoked_at");
        return new AuthSession(
                resultSet.getString("session_id"),
                resultSet.getString("request_id"),
                resultSet.getString("client_id"),
                resultSet.getString("service_id"),
                Instant.parse(resultSet.getString("issued_at")),
                Instant.parse(resultSet.getString("expires_at")),
                AuthSessionStatus.valueOf(resultSet.getString("status")),
                revokedAt == null ? null : Instant.parse(revokedAt));
    }
}
