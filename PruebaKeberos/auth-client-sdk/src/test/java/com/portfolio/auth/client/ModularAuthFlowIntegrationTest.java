package com.portfolio.auth.client;

import com.portfolio.auth.as.AuthenticationHandler;
import com.portfolio.auth.as.InMemoryPrincipalRepository;
import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.dto.ServiceResponse;
import com.portfolio.auth.core.replay.InMemoryReplayCache;
import com.portfolio.auth.crypto.AesGcmCryptoService;
import com.portfolio.auth.service.ProtectedResource;
import com.portfolio.auth.service.ProtectedServiceHandler;
import com.portfolio.auth.tgs.InMemoryServiceRegistry;
import com.portfolio.auth.tgs.TicketGrantingHandler;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.secure.SecureAsResponse;
import com.portfolio.auth.transport.secure.SecureJsonCrypto;
import com.portfolio.auth.transport.secure.SecureTgsResponse;
import com.portfolio.auth.transport.tcp.TcpMessageServer;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModularAuthFlowIntegrationTest {

    @Test
    void shouldCompleteFullModularFlow() throws Exception {
        try (ModularServers servers = ModularServers.start()) {
            AuthClient client = servers.client();

            ServiceResponse response = client.runFullFlow();

            assertTrue(response.accessGranted());
            assertTrue(response.serviceMessage().contains("MODULAR AUTH EXITOSO"));
        }
    }

    @Test
    void shouldBlockRepeatedServiceRequestId() throws Exception {
        try (ModularServers servers = ModularServers.start()) {
            AuthClient client = servers.client();
            SecureAsResponse asResponse = client.requestTicketGrantingTicket();
            SecureTgsResponse tgsResponse = client.requestServiceTicket(asResponse, AuthConfig.DEFAULT_LOCAL_SERVICE_ID);
            String requestId = "service-replay-1";
            String authenticatorId = "service-auth-replay-1";
            Instant issuedAt = Instant.now();

            ServiceResponse first = client.requestProtectedService(tgsResponse, requestId, authenticatorId, issuedAt);
            AuthClientException second = assertThrows(AuthClientException.class,
                    () -> client.requestProtectedService(tgsResponse, requestId, authenticatorId, issuedAt));

            assertTrue(first.accessGranted());
            assertEquals("SERVICE_REPLAY", second.errorResponse().errorCode());
        }
    }

    @Test
    void shouldRejectUnknownServiceAtTgs() throws Exception {
        try (ModularServers servers = ModularServers.start()) {
            AuthClient client = servers.client();
            SecureAsResponse asResponse = client.requestTicketGrantingTicket();

            AuthClientException error = assertThrows(AuthClientException.class,
                    () -> client.requestServiceTicket(asResponse, "missing-service"));

            assertEquals("TGS_UNKNOWN_SERVICE", error.errorResponse().errorCode());
        }
    }

    @Test
    void shouldRejectAuthenticatorOutsideAllowedSkew() throws Exception {
        try (ModularServers servers = ModularServers.start()) {
            AuthClient client = servers.client();
            SecureAsResponse asResponse = client.requestTicketGrantingTicket();
            Instant staleIssuedAt = Instant.now()
                    .minus(AuthConfig.DEFAULT_LOCAL_ALLOWED_CLOCK_SKEW)
                    .minusSeconds(10);

            AuthClientException error = assertThrows(AuthClientException.class,
                    () -> client.requestServiceTicket(
                            asResponse,
                            AuthConfig.DEFAULT_LOCAL_SERVICE_ID,
                            "tgs-invalid-skew-1",
                            "auth-invalid-skew-1",
                            staleIssuedAt));

            assertEquals("TGS_INVALID_AUTHENTICATOR", error.errorResponse().errorCode());
        }
    }

    @Test
    void shouldBlockRepeatedTgsRequestId() throws Exception {
        try (ModularServers servers = ModularServers.start()) {
            AuthClient client = servers.client();
            SecureAsResponse asResponse = client.requestTicketGrantingTicket();
            String requestId = "tgs-replay-1";
            String authenticatorId = "tgs-auth-replay-1";
            Instant issuedAt = Instant.now();

            client.requestServiceTicket(
                    asResponse,
                    AuthConfig.DEFAULT_LOCAL_SERVICE_ID,
                    requestId,
                    authenticatorId,
                    issuedAt);
            AuthClientException second = assertThrows(AuthClientException.class,
                    () -> client.requestServiceTicket(
                            asResponse,
                            AuthConfig.DEFAULT_LOCAL_SERVICE_ID,
                            requestId,
                            authenticatorId,
                            issuedAt));

            assertEquals("TGS_REPLAY", second.errorResponse().errorCode());
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
                    config.legacyPbkdf2Salt());

            TcpMessageServer asServer = new TcpMessageServer(
                    AuthConfig.DEFAULT_LOCAL_HOST,
                    0,
                    codec,
                    new AuthenticationHandler(
                            config,
                            InMemoryPrincipalRepository.fromConfig(config),
                            codec,
                            secureJsonCrypto));
            TcpMessageServer tgsServer = new TcpMessageServer(
                    AuthConfig.DEFAULT_LOCAL_HOST,
                    0,
                    codec,
                    new TicketGrantingHandler(
                            config,
                            InMemoryServiceRegistry.fromConfig(config),
                            new InMemoryReplayCache(),
                            codec,
                            secureJsonCrypto));
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
                            secureJsonCrypto));

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
