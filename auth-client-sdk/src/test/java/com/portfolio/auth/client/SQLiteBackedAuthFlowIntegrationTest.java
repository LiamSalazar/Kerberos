package com.portfolio.auth.client;

import com.portfolio.auth.as.AuthenticationHandler;
import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.replay.InMemoryReplayCache;
import com.portfolio.auth.crypto.AesGcmCryptoService;
import com.portfolio.auth.service.DemoProtectedResource;
import com.portfolio.auth.service.ProtectedServiceHandler;
import com.portfolio.auth.storage.sqlite.SQLitePrincipalRepository;
import com.portfolio.auth.storage.sqlite.SQLiteServiceRegistry;
import com.portfolio.auth.storage.sqlite.SQLiteMigrationRunner;
import com.portfolio.auth.tgs.TicketGrantingHandler;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.protocol.MessageType;
import com.portfolio.auth.transport.secure.SecureJsonCrypto;
import com.portfolio.auth.transport.tcp.TcpMessageServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLiteBackedAuthFlowIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldCompleteFullFlowWithSQLiteBackedRepositories() throws Exception {
        Path database = initializedDatabase();
        try (ModularServers servers = ModularServers.start(database)) {
            AuthClient client = servers.client();

            assertTrue(client.runFullFlow().accessGranted());
        }
    }

    private Path initializedDatabase() throws Exception {
        Path database = tempDir.resolve("auth-flow.sqlite");
        SQLiteMigrationRunner.applyMigrations(
                database,
                Path.of("..", "scripts", "sqlite", "migrations").normalize());
        return database;
    }

    private static final class ModularServers implements AutoCloseable {
        private final TcpMessageServer asServer;
        private final TcpMessageServer tgsServer;
        private final TcpMessageServer serviceServer;
        private final AuthConfig config;

        private ModularServers(
                TcpMessageServer asServer,
                TcpMessageServer tgsServer,
                TcpMessageServer serviceServer,
                AuthConfig config) {
            this.asServer = asServer;
            this.tgsServer = tgsServer;
            this.serviceServer = serviceServer;
            this.config = config;
        }

        private static ModularServers start(Path database) throws Exception {
            AuthConfig config = AuthConfig.localDemo();
            JsonMessageCodec codec = new JsonMessageCodec();
            SecureJsonCrypto secureJsonCrypto = new SecureJsonCrypto(
                    codec,
                    new AesGcmCryptoService(),
                    config.demoPbkdf2Salt());
            SQLiteServiceRegistry serviceRegistry = new SQLiteServiceRegistry(database);

            TcpMessageServer asServer = new TcpMessageServer(
                    AuthConfig.DEFAULT_LOCAL_HOST,
                    0,
                    codec,
                    new AuthenticationHandler(
                            config,
                            new SQLitePrincipalRepository(database),
                            codec,
                            secureJsonCrypto),
                    MessageType.AS_REQUEST);
            TcpMessageServer tgsServer = new TcpMessageServer(
                    AuthConfig.DEFAULT_LOCAL_HOST,
                    0,
                    codec,
                    new TicketGrantingHandler(
                            config,
                            serviceRegistry,
                            new InMemoryReplayCache(),
                            codec,
                            secureJsonCrypto),
                    MessageType.TGS_REQUEST);
            TcpMessageServer serviceServer = new TcpMessageServer(
                    AuthConfig.DEFAULT_LOCAL_HOST,
                    0,
                    codec,
                    new ProtectedServiceHandler(
                            config,
                            serviceRegistry,
                            DemoProtectedResource.fromConfig(config),
                            new InMemoryReplayCache(),
                            codec,
                            secureJsonCrypto),
                    MessageType.SERVICE_REQUEST);

            asServer.start();
            tgsServer.start();
            serviceServer.start();
            return new ModularServers(asServer, tgsServer, serviceServer, config);
        }

        private AuthClient client() {
            return new AuthClient(
                    config,
                    AuthConfig.DEFAULT_LOCAL_HOST,
                    asServer.port(),
                    tgsServer.port(),
                    serviceServer.port());
        }

        @Override
        public void close() {
            serviceServer.close();
            tgsServer.close();
            asServer.close();
        }
    }
}
