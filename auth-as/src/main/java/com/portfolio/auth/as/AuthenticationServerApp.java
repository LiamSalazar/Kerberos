package com.portfolio.auth.as;

import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.health.HealthCheckServer;
import com.portfolio.auth.core.health.HealthCheckSupport;
import com.portfolio.auth.core.observability.MetricsRegistry;
import com.portfolio.auth.core.repository.PrincipalRepository;
import com.portfolio.auth.crypto.AesGcmCryptoService;
import com.portfolio.auth.storage.postgres.PostgresConnectionFactory;
import com.portfolio.auth.storage.postgres.PostgresMigrationRunner;
import com.portfolio.auth.storage.postgres.PostgresPrincipalRepository;
import com.portfolio.auth.storage.sqlite.SQLitePrincipalRepository;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.protocol.MessageType;
import com.portfolio.auth.transport.secure.SecureJsonCrypto;
import com.portfolio.auth.transport.tcp.TcpMessageServer;

public final class AuthenticationServerApp {
    private static final String ENV_HEALTH_HOST = "AUTH_HEALTH_HOST";
    private static final String ENV_HEALTH_PORT = "AUTH_HEALTH_PORT";
    private static final int DEFAULT_HEALTH_PORT = 2900;

    private AuthenticationServerApp() {
    }

    public static void main(String[] args) throws Exception {
        AuthConfig config = AuthConfig.fromEnvironment();
        AuthConfig.printDemoWarningIfNeeded(System.getenv(), config);
        JsonMessageCodec codec = new JsonMessageCodec();
        PrincipalRepository principals = principalRepository(config);
        AuthenticationHandler handler = new AuthenticationHandler(
                config,
                principals,
                codec,
                new SecureJsonCrypto(codec, new AesGcmCryptoService(), config.demoPbkdf2Salt()));
        TcpMessageServer server = new TcpMessageServer(
                config.authenticationServerHost(),
                config.authenticationServerPort(),
                codec,
                handler,
                MessageType.AS_REQUEST);

        server.start();
        HealthCheckServer healthServer = HealthCheckServer.start(
                value(ENV_HEALTH_HOST, AuthConfig.DEFAULT_LOCAL_HOST),
                intValue(ENV_HEALTH_PORT, DEFAULT_HEALTH_PORT),
                new HealthCheckSupport("auth-as", "0.1.0-SNAPSHOT", config.storageMode()),
                MetricsRegistry.global());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            healthServer.close();
            server.close();
        }));
        System.out.println("[auth-as] Modular AS escuchando en " + config.authenticationServerPort());
        Thread.currentThread().join();
    }

    private static PrincipalRepository principalRepository(AuthConfig config) throws Exception {
        if (config.usesPostgresStorage()) {
            PostgresConnectionFactory connectionFactory = new PostgresConnectionFactory(config);
            PostgresMigrationRunner.applyMigrations(connectionFactory);
            return new PostgresPrincipalRepository(connectionFactory);
        }
        if (config.usesSqliteStorage()) {
            return new SQLitePrincipalRepository(java.nio.file.Path.of(config.sqlitePath()));
        }
        return InMemoryPrincipalRepository.fromConfig(config);
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
