package com.portfolio.auth.client;

import com.portfolio.auth.as.AuthenticationHandler;
import com.portfolio.auth.as.InMemoryPrincipalRepository;
import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.AsRequest;
import com.portfolio.auth.core.protocol.dto.ClientAuthenticator;
import com.portfolio.auth.core.protocol.dto.ErrorResponse;
import com.portfolio.auth.core.protocol.dto.ServiceResponse;
import com.portfolio.auth.core.protocol.dto.TicketService;
import com.portfolio.auth.core.protocol.dto.TicketTgs;
import com.portfolio.auth.core.replay.InMemoryReplayCache;
import com.portfolio.auth.crypto.AesGcmCryptoService;
import com.portfolio.auth.crypto.CryptoEnvelope;
import com.portfolio.auth.service.ProtectedResource;
import com.portfolio.auth.service.ProtectedServiceHandler;
import com.portfolio.auth.tgs.InMemoryServiceRegistry;
import com.portfolio.auth.tgs.TicketGrantingHandler;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.protocol.MessageType;
import com.portfolio.auth.transport.protocol.ProtocolEnvelope;
import com.portfolio.auth.transport.secure.SecureAad;
import com.portfolio.auth.transport.secure.SecureAsResponse;
import com.portfolio.auth.transport.secure.SecureJsonCrypto;
import com.portfolio.auth.transport.secure.SecureServiceRequest;
import com.portfolio.auth.transport.secure.SecureTgsRequest;
import com.portfolio.auth.transport.secure.SecureTgsResponse;
import com.portfolio.auth.transport.tcp.TcpMessageClient;
import com.portfolio.auth.transport.tcp.TcpMessageServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
    void shouldRejectUnknownClientAtAs() throws Exception {
        try (ModularServers servers = ModularServers.start()) {
            AsRequest request = new AsRequest(
                    ProtocolDefaults.CURRENT_VERSION,
                    "as-unknown-client-1",
                    "missing-client",
                    AuthConfig.DEFAULT_LOCAL_TGS_ID,
                    Instant.now());

            ProtocolEnvelope response = servers.sendToAs(MessageType.AS_REQUEST, request.requestId(), request);
            ErrorResponse error = servers.error(response);

            assertEquals("AS_UNKNOWN_PRINCIPAL", error.errorCode());
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

    @Test
    void shouldRejectExpiredTicketTgs() throws Exception {
        try (ModularServers servers = ModularServers.start()) {
            Instant now = Instant.now();
            String requestId = "tgs-expired-ticket-1";
            TicketTgs ticket = new TicketTgs(
                    ProtocolDefaults.CURRENT_VERSION,
                    "expired-tgt-1",
                    now.minus(Duration.ofMinutes(10)),
                    now.minusSeconds(1),
                    servers.config.defaultClientId(),
                    servers.config.clientHost(),
                    servers.config.defaultTicketGrantingServerId(),
                    "expired-tgs-session");
            ClientAuthenticator authenticator = new ClientAuthenticator(
                    ProtocolDefaults.CURRENT_VERSION,
                    "auth-expired-ticket-1",
                    now,
                    now.plus(servers.config.replayWindow()),
                    servers.config.defaultClientId(),
                    servers.config.clientHost());
            SecureTgsRequest request = new SecureTgsRequest(
                    ProtocolDefaults.CURRENT_VERSION,
                    requestId,
                    now,
                    servers.config.defaultClientId(),
                    servers.config.defaultServiceId(),
                    servers.config.defaultTicketGrantingServerId(),
                    servers.secureJsonCrypto.encrypt(
                            ticket,
                            servers.config.legacyTicketGrantingServerSecret(),
                            SecureAad.ticketTgs()),
                    servers.secureJsonCrypto.encrypt(
                            authenticator,
                            ticket.clientTgsSessionKey(),
                            SecureAad.authenticator(requestId)));

            ProtocolEnvelope response = servers.sendToTgs(MessageType.TGS_REQUEST, request.requestId(), request);
            ErrorResponse error = servers.error(response);

            assertEquals("TGS_INVALID_AUTHENTICATOR", error.errorCode());
        }
    }

    @Test
    void shouldRejectExpiredServiceTicket() throws Exception {
        try (ModularServers servers = ModularServers.start()) {
            Instant now = Instant.now();
            String requestId = "service-expired-ticket-1";
            TicketService ticket = new TicketService(
                    ProtocolDefaults.CURRENT_VERSION,
                    "expired-service-ticket-1",
                    now.minus(Duration.ofMinutes(10)),
                    now.minusSeconds(1),
                    servers.config.defaultClientId(),
                    servers.config.clientHost(),
                    servers.config.defaultServiceId(),
                    "expired-service-session");
            ClientAuthenticator authenticator = new ClientAuthenticator(
                    ProtocolDefaults.CURRENT_VERSION,
                    "auth-expired-service-ticket-1",
                    now,
                    now.plus(servers.config.replayWindow()),
                    servers.config.defaultClientId(),
                    servers.config.clientHost());
            SecureServiceRequest request = secureServiceRequest(servers, requestId, now, ticket, authenticator);

            ProtocolEnvelope response = servers.sendToService(
                    MessageType.SERVICE_REQUEST,
                    request.requestId(),
                    request);
            ErrorResponse error = servers.error(response);

            assertEquals("SERVICE_INVALID_AUTHENTICATOR", error.errorCode());
        }
    }

    @Test
    void shouldRejectExpiredServiceAuthenticator() throws Exception {
        try (ModularServers servers = ModularServers.start()) {
            Instant now = Instant.now();
            String requestId = "service-expired-auth-1";
            TicketService ticket = new TicketService(
                    ProtocolDefaults.CURRENT_VERSION,
                    "valid-service-ticket-1",
                    now,
                    now.plus(servers.config.ticketLifetime()),
                    servers.config.defaultClientId(),
                    servers.config.clientHost(),
                    servers.config.defaultServiceId(),
                    "valid-service-session");
            ClientAuthenticator authenticator = new ClientAuthenticator(
                    ProtocolDefaults.CURRENT_VERSION,
                    "auth-expired-service-1",
                    now.minusSeconds(10),
                    now.minusSeconds(1),
                    servers.config.defaultClientId(),
                    servers.config.clientHost());
            SecureServiceRequest request = secureServiceRequest(servers, requestId, now, ticket, authenticator);

            ProtocolEnvelope response = servers.sendToService(
                    MessageType.SERVICE_REQUEST,
                    request.requestId(),
                    request);
            ErrorResponse error = servers.error(response);

            assertEquals("SERVICE_INVALID_AUTHENTICATOR", error.errorCode());
        }
    }

    @Test
    void shouldRejectUnexpectedMessageTypeAtTransport() throws Exception {
        try (ModularServers servers = ModularServers.start()) {
            AsRequest request = new AsRequest(
                    ProtocolDefaults.CURRENT_VERSION,
                    "as-wrong-type-1",
                    AuthConfig.DEFAULT_LOCAL_CLIENT_ID,
                    AuthConfig.DEFAULT_LOCAL_TGS_ID,
                    Instant.now());

            ProtocolEnvelope response = servers.sendToAs(MessageType.TGS_REQUEST, request.requestId(), request);
            ErrorResponse error = servers.error(response);

            assertEquals("TRANSPORT_UNEXPECTED_MESSAGE", error.errorCode());
        }
    }

    @Test
    void shouldFailClearlyWhenServerIsUnavailable() throws Exception {
        int unavailablePort = freePort();
        TcpMessageClient client = new TcpMessageClient(
                new JsonMessageCodec(),
                Duration.ofMillis(100),
                Duration.ofMillis(100));
        AsRequest request = new AsRequest(
                ProtocolDefaults.CURRENT_VERSION,
                "as-unavailable-1",
                AuthConfig.DEFAULT_LOCAL_CLIENT_ID,
                AuthConfig.DEFAULT_LOCAL_TGS_ID,
                Instant.now());
        ProtocolEnvelope envelope = new ProtocolEnvelope(
                MessageType.AS_REQUEST,
                ProtocolDefaults.CURRENT_VERSION,
                request.requestId(),
                request.issuedAt(),
                new JsonMessageCodec().encodePayload(request));

        assertThrows(IOException.class,
                () -> client.send(AuthConfig.DEFAULT_LOCAL_HOST, unavailablePort, envelope));
    }

    @Test
    void shouldServeMultipleConcurrentClients() throws Exception {
        try (ModularServers servers = ModularServers.start()) {
            ExecutorService executor = Executors.newFixedThreadPool(4);
            try {
                List<Future<ServiceResponse>> futures = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    futures.add(executor.submit(() -> servers.client().runFullFlow()));
                }

                for (Future<ServiceResponse> future : futures) {
                    ServiceResponse response = future.get(5, TimeUnit.SECONDS);
                    assertTrue(response.accessGranted());
                    assertTrue(response.serviceMessage().contains("MODULAR AUTH EXITOSO"));
                }
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private static SecureServiceRequest secureServiceRequest(
            ModularServers servers,
            String requestId,
            Instant issuedAt,
            TicketService ticket,
            ClientAuthenticator authenticator) throws Exception {
        CryptoEnvelope ticketEnvelope = servers.secureJsonCrypto.encrypt(
                ticket,
                servers.config.legacyServiceSecret(),
                SecureAad.ticketService());
        CryptoEnvelope authenticatorEnvelope = servers.secureJsonCrypto.encrypt(
                authenticator,
                ticket.clientServiceSessionKey(),
                SecureAad.authenticator(requestId));
        return new SecureServiceRequest(
                ProtocolDefaults.CURRENT_VERSION,
                requestId,
                issuedAt,
                servers.config.defaultClientId(),
                servers.config.defaultServiceId(),
                ticketEnvelope,
                authenticatorEnvelope);
    }

    private static int freePort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

    private static final class ModularServers implements AutoCloseable {
        private final TcpMessageServer asServer;
        private final TcpMessageServer tgsServer;
        private final TcpMessageServer serviceServer;
        private final AuthConfig config;
        private final JsonMessageCodec codec;
        private final SecureJsonCrypto secureJsonCrypto;

        private ModularServers(
                TcpMessageServer asServer,
                TcpMessageServer tgsServer,
                TcpMessageServer serviceServer,
                AuthConfig config,
                JsonMessageCodec codec,
                SecureJsonCrypto secureJsonCrypto) {
            this.asServer = asServer;
            this.tgsServer = tgsServer;
            this.serviceServer = serviceServer;
            this.config = config;
            this.codec = codec;
            this.secureJsonCrypto = secureJsonCrypto;
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
            return new ModularServers(asServer, tgsServer, serviceServer, config, codec, secureJsonCrypto);
        }

        private AuthClient client() {
            return new AuthClient(
                    config,
                    AuthConfig.DEFAULT_LOCAL_HOST,
                    asServer.port(),
                    tgsServer.port(),
                    serviceServer.port());
        }

        private ProtocolEnvelope sendToAs(MessageType type, String requestId, Object payload) throws IOException {
            return send(asServer.port(), type, requestId, payload);
        }

        private ProtocolEnvelope sendToTgs(MessageType type, String requestId, Object payload) throws IOException {
            return send(tgsServer.port(), type, requestId, payload);
        }

        private ProtocolEnvelope sendToService(MessageType type, String requestId, Object payload) throws IOException {
            return send(serviceServer.port(), type, requestId, payload);
        }

        private ProtocolEnvelope send(int port, MessageType type, String requestId, Object payload) throws IOException {
            ProtocolEnvelope envelope = new ProtocolEnvelope(
                    type,
                    ProtocolDefaults.CURRENT_VERSION,
                    requestId,
                    Instant.now(),
                    codec.encodePayload(payload));
            return new TcpMessageClient(codec).send(AuthConfig.DEFAULT_LOCAL_HOST, port, envelope);
        }

        private ErrorResponse error(ProtocolEnvelope response) {
            assertEquals(MessageType.ERROR_RESPONSE, response.messageType());
            return codec.decodePayload(response.payloadJson(), ErrorResponse.class);
        }

        @Override
        public void close() {
            serviceServer.close();
            tgsServer.close();
            asServer.close();
        }
    }
}
