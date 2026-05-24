package com.portfolio.auth.service;

import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.health.HealthCheckServer;
import com.portfolio.auth.core.health.HealthCheckSupport;
import com.portfolio.auth.core.observability.MetricsRegistry;
import com.portfolio.auth.core.repository.InMemoryServiceRegistry;
import com.portfolio.auth.core.repository.ServiceRegistry;
import com.portfolio.auth.core.replay.InMemoryReplayCache;
import com.portfolio.auth.crypto.AesGcmCryptoService;
import com.portfolio.auth.storage.postgres.PostgresConnectionFactory;
import com.portfolio.auth.storage.postgres.PostgresMigrationRunner;
import com.portfolio.auth.storage.postgres.PostgresServiceRegistry;
import com.portfolio.auth.storage.sqlite.SQLiteServiceRegistry;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.protocol.MessageType;
import com.portfolio.auth.transport.secure.SecureJsonCrypto;
import com.portfolio.auth.transport.tcp.TcpMessageServer;

public final class ProtectedServiceApp {
    private static final String ENV_HEALTH_HOST = "AUTH_HEALTH_HOST";
    private static final String ENV_HEALTH_PORT = "AUTH_HEALTH_PORT";
    private static final int DEFAULT_HEALTH_PORT = 2902;

    private ProtectedServiceApp() {
    }

    public static void main(String[] args) throws Exception {
        AuthConfig config = AuthConfig.fromEnvironment();
        AuthConfig.printDemoWarningIfNeeded(System.getenv(), config);
        JsonMessageCodec codec = new JsonMessageCodec();
        ServiceRegistry registry = serviceRegistry(config);
        ProtectedServiceHandler handler = new ProtectedServiceHandler(
                config,
                registry,
                DemoProtectedResource.fromConfig(config),
                new InMemoryReplayCache(),
                codec,
                new SecureJsonCrypto(codec, new AesGcmCryptoService(), config.demoPbkdf2Salt()));
        TcpMessageServer server = new TcpMessageServer(
                config.serviceServerHost(),
                config.serviceServerPort(),
                codec,
                handler,
                MessageType.SERVICE_REQUEST);

        server.start();
        HealthCheckServer healthServer = HealthCheckServer.start(
                value(ENV_HEALTH_HOST, AuthConfig.DEFAULT_LOCAL_HOST),
                intValue(ENV_HEALTH_PORT, DEFAULT_HEALTH_PORT),
                new HealthCheckSupport("auth-service", "0.1.0-SNAPSHOT", config.storageMode()),
                MetricsRegistry.global());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            healthServer.close();
            server.close();
        }));
        System.out.println("[auth-service] Servicio modular escuchando en " + config.serviceServerPort());
        Thread.currentThread().join();
    }

    private static ServiceRegistry serviceRegistry(AuthConfig config) throws Exception {
        if (config.usesPostgresStorage()) {
            PostgresConnectionFactory connectionFactory = new PostgresConnectionFactory(config);
            PostgresMigrationRunner.applyMigrations(connectionFactory);
            return new PostgresServiceRegistry(connectionFactory);
        }
        if (config.usesSqliteStorage()) {
            return new SQLiteServiceRegistry(java.nio.file.Path.of(config.sqlitePath()));
        }
        return InMemoryServiceRegistry.fromConfig(config);
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
}
