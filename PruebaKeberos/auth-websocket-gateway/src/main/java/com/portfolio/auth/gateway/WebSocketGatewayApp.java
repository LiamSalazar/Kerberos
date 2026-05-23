package com.portfolio.auth.gateway;

import com.portfolio.auth.client.AuthClient;
import com.portfolio.auth.core.audit.AuthEventRepository;
import com.portfolio.auth.core.audit.NoOpAuthEventRepository;
import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.session.InMemorySessionRepository;
import com.portfolio.auth.core.session.SessionRepository;
import com.portfolio.auth.storage.sqlite.SQLiteAuditRepository;
import com.portfolio.auth.storage.sqlite.SQLiteMigrationRunner;
import com.portfolio.auth.storage.sqlite.SQLiteSessionRepository;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class WebSocketGatewayApp {
    public static final String ENV_WEBSOCKET_HOST = "AUTH_WS_HOST";
    public static final String ENV_WEBSOCKET_PORT = "AUTH_WS_PORT";
    public static final String ENV_ALLOWED_ORIGINS = "AUTH_ALLOWED_ORIGINS";
    public static final int DEFAULT_WEBSOCKET_PORT = 2800;

    private WebSocketGatewayApp() {
    }

    public static void main(String[] args) throws Exception {
        AuthConfig config = AuthConfig.fromEnvironment();
        AuthConfig.printDemoWarningIfNeeded(System.getenv(), config);
        String host = value(ENV_WEBSOCKET_HOST, AuthConfig.DEFAULT_LOCAL_HOST);
        int port = intValue(ENV_WEBSOCKET_PORT, DEFAULT_WEBSOCKET_PORT);

        AuthClient authClient = new AuthClient(config);
        AuthEventRepository auditRepository = auditRepository(config);
        SessionRepository sessionRepository = sessionRepository(config);
        GatewaySessionService sessionService = new GatewaySessionService(config, sessionRepository);
        WebSocketGatewayPolicy policy = new WebSocketGatewayPolicy(
                allowedOrigins(value(ENV_ALLOWED_ORIGINS, "")),
                WebSocketGatewayPolicy.DEFAULT_MAX_MESSAGES_PER_CONNECTION,
                WebSocketGatewayPolicy.DEFAULT_RATE_LIMIT_WINDOW,
                WebSocketGatewayPolicy.DEFAULT_FLOW_TIMEOUT);
        GatewayAuthFlowService flowService = new GatewayAuthFlowService(
                config,
                new DefaultGatewayAuthClient(config, authClient),
                auditRepository,
                sessionService);
        AuthWebSocketServer server = new AuthWebSocketServer(
                new InetSocketAddress(host, port),
                new WebSocketMessageCodec(),
                new WebSocketMessageProcessor(flowService, sessionService),
                policy);

        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        Thread.currentThread().join();
    }

    private static AuthEventRepository auditRepository(AuthConfig config) throws Exception {
        if (!config.usesSqliteStorage()) {
            return NoOpAuthEventRepository.INSTANCE;
        }
        Path databasePath = Path.of(config.sqlitePath());
        SQLiteMigrationRunner.applyMigrations(databasePath);
        return new SQLiteAuditRepository(databasePath);
    }

    private static SessionRepository sessionRepository(AuthConfig config) throws Exception {
        if (!config.usesSqliteSessionStorage()) {
            return new InMemorySessionRepository();
        }
        Path databasePath = Path.of(config.sqlitePath());
        SQLiteMigrationRunner.applyMigrations(databasePath);
        return new SQLiteSessionRepository(databasePath);
    }

    private static String value(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static int intValue(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static Set<String> allowedOrigins(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
