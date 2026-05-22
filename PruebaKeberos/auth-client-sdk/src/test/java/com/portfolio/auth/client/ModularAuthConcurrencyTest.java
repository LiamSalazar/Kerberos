package com.portfolio.auth.client;

import com.portfolio.auth.as.AuthenticationHandler;
import com.portfolio.auth.as.InMemoryPrincipalRepository;
import com.portfolio.auth.client.audit.ModularAuthConcurrencyAuditRunner;
import com.portfolio.auth.client.audit.ModularAuthConcurrencyAuditRunner.ConcurrencyReport;
import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.repository.InMemoryServiceRegistry;
import com.portfolio.auth.core.replay.InMemoryReplayCache;
import com.portfolio.auth.crypto.AesGcmCryptoService;
import com.portfolio.auth.service.ProtectedResource;
import com.portfolio.auth.service.ProtectedServiceHandler;
import com.portfolio.auth.tgs.TicketGrantingHandler;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.protocol.MessageType;
import com.portfolio.auth.transport.secure.SecureJsonCrypto;
import com.portfolio.auth.transport.tcp.TcpMessageServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModularAuthConcurrencyTest {
    private static final int INITIAL_CONCURRENT_CLIENTS = 10;
    private static final int INITIAL_TOTAL_FLOWS = 50;

    @Test
    void shouldCompleteConcurrentAuthFlowsWithoutReplayCollisions() throws Exception {
        try (ModularServers servers = ModularServers.start()) {
            ConcurrencyReport report = ModularAuthConcurrencyAuditRunner.run(
                    servers::client,
                    servers.config,
                    INITIAL_CONCURRENT_CLIENTS,
                    INITIAL_TOTAL_FLOWS,
                    "mvn test ModularAuthConcurrencyTest");

            assertEquals(INITIAL_TOTAL_FLOWS, report.totalFlows());
            assertEquals(INITIAL_CONCURRENT_CLIENTS, report.concurrentClients());
            assertEquals(INITIAL_TOTAL_FLOWS, report.successes());
            assertEquals(0, report.failures());
            assertTrue(report.throughputPerSecond() > 0);
            assertTrue(report.totalSummary().averageMillis() > 0);
            assertTrue(report.totalP95Millis() > 0);
        }
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

        private static ModularServers start() throws Exception {
            AuthConfig config = AuthConfig.localDemo();
            JsonMessageCodec codec = new JsonMessageCodec();
            SecureJsonCrypto secureJsonCrypto = new SecureJsonCrypto(
                    codec,
                    new AesGcmCryptoService(),
                    config.demoPbkdf2Salt());

            TcpMessageServer asServer = new TcpMessageServer(
                    AuthConfig.DEFAULT_LOCAL_HOST,
                    0,
                    codec,
                    new AuthenticationHandler(
                            config,
                            InMemoryPrincipalRepository.fromConfig(config),
                            codec,
                            secureJsonCrypto),
                    MessageType.AS_REQUEST);
            TcpMessageServer tgsServer = new TcpMessageServer(
                    AuthConfig.DEFAULT_LOCAL_HOST,
                    0,
                    codec,
                    new TicketGrantingHandler(
                            config,
                            InMemoryServiceRegistry.fromConfig(config),
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
                            ProtectedServiceHandler.defaultSecrets(config),
                            ProtectedResource.demo(),
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
