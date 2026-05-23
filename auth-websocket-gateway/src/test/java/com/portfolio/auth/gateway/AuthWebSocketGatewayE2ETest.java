package com.portfolio.auth.gateway;

import com.portfolio.auth.as.AuthenticationHandler;
import com.portfolio.auth.as.InMemoryPrincipalRepository;
import com.portfolio.auth.client.AuthClient;
import com.portfolio.auth.core.audit.NoOpAuthEventRepository;
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
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class AuthWebSocketGatewayE2ETest {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private final WebSocketMessageCodec codec = new WebSocketMessageCodec();

    @Test
    void shouldRunFullAuthFlowThroughRealWebSocketClient() throws Exception {
        try (ModularServers servers = ModularServers.start();
                GatewayServer gateway = GatewayServer.start(servers.client());
                RecordingWebSocketClient client = RecordingWebSocketClient.connect(gateway.port(), codec)) {
            client.send("""
                    {"type":"START_AUTH_FLOW","requestId":"e2e-test-1","clientId":"1","serviceId":"1"}
                    """);

            List<WebSocketMessage> messages = client.awaitUntil(WebSocketMessageType.FLOW_RESULT);
            WebSocketMessage result = messages.get(messages.size() - 1);

            assertEquals("e2e-test-1", result.requestId());
            assertTrue(result.success());
            assertEquals(null, result.errorType());
            assertNotNull(result.sessionId());
            assertNotNull(result.sessionExpiresAt());
            assertTrue(result.serviceMessage().contains("MODULAR AUTH EXITOSO"));
            assertNotNull(result.totalMillis());
            assertRequiredStages(messages);
            assertNoSensitiveLeak(messages);

            client.send("""
                    {"type":"VERIFY_SESSION","requestId":"verify-e2e-1","sessionId":"%s","clientId":"1","serviceId":"1"}
                    """.formatted(result.sessionId()));

            WebSocketMessage valid = client.awaitType(WebSocketMessageType.SESSION_VALID);
            assertEquals("verify-e2e-1", valid.requestId());
            assertEquals(true, valid.valid());
            assertEquals("1", valid.clientId());
            assertEquals("1", valid.serviceId());
        }
    }

    @Test
    void shouldReturnErrorForInvalidJson() throws Exception {
        try (GatewayServer gateway = GatewayServer.startUnavailable();
                RecordingWebSocketClient client = RecordingWebSocketClient.connect(gateway.port(), codec)) {
            client.send("{\"type\":");

            WebSocketMessage error = client.awaitType(WebSocketMessageType.ERROR);
            assertEquals(WebSocketErrorType.INVALID_JSON.name(), error.errorType());
            assertTrue(error.message().contains("JSON") || error.message().contains("Se esperaba"));
        }
    }

    @Test
    void shouldReturnErrorForUnknownMessageType() throws Exception {
        try (GatewayServer gateway = GatewayServer.startUnavailable();
                RecordingWebSocketClient client = RecordingWebSocketClient.connect(gateway.port(), codec)) {
            client.send("{\"type\":\"UNKNOWN\",\"requestId\":\"bad-type\"}");

            WebSocketMessage error = client.awaitType(WebSocketMessageType.ERROR);
            assertEquals(WebSocketErrorType.UNKNOWN_MESSAGE_TYPE.name(), error.errorType());
            assertTrue(error.message().contains("WebSocketMessageType no soportado"));
        }
    }

    @Test
    void shouldReturnErrorWhenTypeIsMissing() throws Exception {
        try (GatewayServer gateway = GatewayServer.startUnavailable();
                RecordingWebSocketClient client = RecordingWebSocketClient.connect(gateway.port(), codec)) {
            client.send("{}");

            WebSocketMessage error = client.awaitType(WebSocketMessageType.ERROR);
            assertEquals(WebSocketErrorType.MISSING_REQUIRED_FIELD.name(), error.errorType());
            assertTrue(error.message().contains("type"));
        }
    }

    @Test
    void shouldRejectMissingStartAuthFlowFields() throws Exception {
        try (GatewayServer gateway = GatewayServer.startUnavailable();
                RecordingWebSocketClient client = RecordingWebSocketClient.connect(gateway.port(), codec)) {
            client.send("""
                    {"type":"START_AUTH_FLOW","requestId":"missing-fields","clientId":"1"}
                    """);

            WebSocketMessage error = client.awaitType(WebSocketMessageType.ERROR);
            assertEquals(WebSocketErrorType.MISSING_REQUIRED_FIELD.name(), error.errorType());
            assertTrue(error.message().contains("serviceId"));
        }
    }

    @Test
    void shouldRejectUnknownClientThroughGateway() throws Exception {
        try (ModularServers servers = ModularServers.start();
                GatewayServer gateway = GatewayServer.start(servers.client());
                RecordingWebSocketClient client = RecordingWebSocketClient.connect(gateway.port(), codec)) {

            client.send("""
                    {"type":"START_AUTH_FLOW","requestId":"unknown-client","clientId":"missing","serviceId":"1"}
                    """);

            WebSocketMessage result = client.awaitType(WebSocketMessageType.FLOW_RESULT);
            assertFalse(result.success());
            assertEquals(WebSocketErrorType.CLIENT_NOT_FOUND.name(), result.errorType());
            assertEquals(null, result.sessionId());
            assertTrue(result.serviceMessage().contains("Cliente no registrado"));
        }
    }

    @Test
    void shouldRejectUnknownServiceThroughGateway() throws Exception {
        try (ModularServers servers = ModularServers.start();
                GatewayServer gateway = GatewayServer.start(servers.client());
                RecordingWebSocketClient client = RecordingWebSocketClient.connect(gateway.port(), codec)) {

            client.send("""
                    {"type":"START_AUTH_FLOW","requestId":"unknown-service","clientId":"1","serviceId":"missing"}
                    """);

            WebSocketMessage result = client.awaitType(WebSocketMessageType.FLOW_RESULT);
            assertFalse(result.success());
            assertEquals(WebSocketErrorType.SERVICE_NOT_FOUND.name(), result.errorType());
            assertEquals(null, result.sessionId());
            assertTrue(result.serviceMessage().contains("TGS_UNKNOWN_SERVICE"));
        }
    }

    @Test
    void shouldLogoutSessionThroughGateway() throws Exception {
        try (ModularServers servers = ModularServers.start();
                GatewayServer gateway = GatewayServer.start(servers.client());
                RecordingWebSocketClient client = RecordingWebSocketClient.connect(gateway.port(), codec)) {

            client.send("""
                    {"type":"START_AUTH_FLOW","requestId":"logout-flow","clientId":"1","serviceId":"1"}
                    """);
            WebSocketMessage result = client.awaitType(WebSocketMessageType.FLOW_RESULT);
            assertTrue(result.success());

            client.send("""
                    {"type":"LOGOUT_SESSION","requestId":"logout-session","sessionId":"%s"}
                    """.formatted(result.sessionId()));
            WebSocketMessage loggedOut = client.awaitType(WebSocketMessageType.SESSION_LOGGED_OUT);
            assertEquals("logout-session", loggedOut.requestId());

            client.send("""
                    {"type":"VERIFY_SESSION","requestId":"verify-after-logout","sessionId":"%s","clientId":"1","serviceId":"1"}
                    """.formatted(result.sessionId()));
            WebSocketMessage invalid = client.awaitType(WebSocketMessageType.SESSION_INVALID);
            assertEquals("REVOKED", invalid.reason());
        }
    }

    @Test
    void shouldReturnControlledFailureWhenServicesAreUnavailable() throws Exception {
        try (GatewayServer gateway = GatewayServer.startUnavailable();
                RecordingWebSocketClient client = RecordingWebSocketClient.connect(gateway.port(), codec)) {
            client.send("""
                    {"type":"START_AUTH_FLOW","requestId":"services-down","clientId":"1","serviceId":"1"}
                    """);

            List<WebSocketMessage> messages = client.awaitUntil(WebSocketMessageType.FLOW_RESULT);
            WebSocketMessage result = messages.get(messages.size() - 1);

            assertFalse(result.success());
            assertEquals(WebSocketErrorType.FLOW_FAILED.name(), result.errorType());
            assertEquals(null, result.sessionId());
            assertTrue(messages.stream().anyMatch(message -> "FLOW_ERROR".equals(message.stage())));
            assertNoSensitiveLeak(messages);
        }
    }

    @Test
    void shouldAcceptAllowedOrigin() throws Exception {
        WebSocketGatewayPolicy policy = new WebSocketGatewayPolicy(
                Set.of("http://allowed.local"),
                WebSocketGatewayPolicy.DEFAULT_MAX_MESSAGES_PER_CONNECTION,
                WebSocketGatewayPolicy.DEFAULT_RATE_LIMIT_WINDOW,
                WebSocketGatewayPolicy.DEFAULT_FLOW_TIMEOUT);
        try (GatewayServer gateway = GatewayServer.startUnavailable(policy);
                RecordingWebSocketClient client = RecordingWebSocketClient.connect(
                        gateway.port(),
                        codec,
                        Map.of("Origin", "http://allowed.local"))) {
            client.send("""
                    {"type":"PING","requestId":"origin-ok"}
                    """);

            WebSocketMessage pong = client.awaitType(WebSocketMessageType.PONG);
            assertEquals("origin-ok", pong.requestId());
        }
    }

    @Test
    void shouldRejectDisallowedOrigin() throws Exception {
        WebSocketGatewayPolicy policy = new WebSocketGatewayPolicy(
                Set.of("http://allowed.local"),
                WebSocketGatewayPolicy.DEFAULT_MAX_MESSAGES_PER_CONNECTION,
                WebSocketGatewayPolicy.DEFAULT_RATE_LIMIT_WINDOW,
                WebSocketGatewayPolicy.DEFAULT_FLOW_TIMEOUT);
        try (GatewayServer gateway = GatewayServer.startUnavailable(policy);
                RecordingWebSocketClient client = RecordingWebSocketClient.connect(
                        gateway.port(),
                        codec,
                        Map.of("Origin", "http://blocked.local"))) {
            WebSocketMessage error = client.awaitType(WebSocketMessageType.ERROR);
            assertEquals(WebSocketErrorType.ORIGIN_NOT_ALLOWED.name(), error.errorType());
        }
    }

    @Test
    void shouldRateLimitConnection() throws Exception {
        WebSocketGatewayPolicy policy = new WebSocketGatewayPolicy(
                Set.of(),
                1,
                Duration.ofSeconds(10),
                WebSocketGatewayPolicy.DEFAULT_FLOW_TIMEOUT);
        try (GatewayServer gateway = GatewayServer.startUnavailable(policy);
                RecordingWebSocketClient client = RecordingWebSocketClient.connect(gateway.port(), codec)) {
            client.send("""
                    {"type":"PING","requestId":"rate-1"}
                    """);
            assertEquals(WebSocketMessageType.PONG, client.awaitType(WebSocketMessageType.PONG).type());

            client.send("""
                    {"type":"PING","requestId":"rate-2"}
                    """);

            WebSocketMessage error = client.awaitType(WebSocketMessageType.ERROR);
            assertEquals(WebSocketErrorType.RATE_LIMITED.name(), error.errorType());
        }
    }

    @Test
    void shouldTimeoutLongRunningFlow() throws Exception {
        WebSocketGatewayPolicy policy = new WebSocketGatewayPolicy(
                Set.of(),
                WebSocketGatewayPolicy.DEFAULT_MAX_MESSAGES_PER_CONNECTION,
                WebSocketGatewayPolicy.DEFAULT_RATE_LIMIT_WINDOW,
                Duration.ofMillis(50));
        try (GatewayServer gateway = GatewayServer.start(newSlowGatewayClient(), policy);
                RecordingWebSocketClient client = RecordingWebSocketClient.connect(gateway.port(), codec)) {
            client.send("""
                    {"type":"START_AUTH_FLOW","requestId":"flow-timeout","clientId":"1","serviceId":"1"}
                    """);

            WebSocketMessage error = client.awaitType(WebSocketMessageType.ERROR);
            assertEquals(WebSocketErrorType.FLOW_FAILED.name(), error.errorType());
            assertTrue(error.message().contains("Timeout"));
        }
    }

    private static void assertRequiredStages(List<WebSocketMessage> messages) {
        Set<String> stages = messages.stream()
                .filter(message -> message.type() == WebSocketMessageType.FLOW_EVENT)
                .map(WebSocketMessage::stage)
                .collect(Collectors.toSet());
        Set<WebSocketFlowStage> expected = EnumSet.of(
                WebSocketFlowStage.FLOW_STARTED,
                WebSocketFlowStage.AS_REQUEST_SENT,
                WebSocketFlowStage.AS_RESPONSE_RECEIVED,
                WebSocketFlowStage.TGS_REQUEST_SENT,
                WebSocketFlowStage.TGS_RESPONSE_RECEIVED,
                WebSocketFlowStage.SERVICE_REQUEST_SENT,
                WebSocketFlowStage.SERVICE_RESPONSE_RECEIVED,
                WebSocketFlowStage.FLOW_SUCCESS);

        for (WebSocketFlowStage stage : expected) {
            assertTrue(stages.contains(stage.name()), "Falta evento " + stage);
        }
    }

    private static void assertNoSensitiveLeak(List<WebSocketMessage> messages) {
        for (WebSocketMessage message : messages) {
            String text = String.join(
                    " ",
                    String.valueOf(message.message()),
                    String.valueOf(message.serviceMessage()))
                    .toLowerCase();
            assertFalse(text.contains("ciphertext"), "No se deben exponer ciphertexts");
            assertFalse(text.contains("clienttgs"), "No se deben exponer claves de sesion TGS");
            assertFalse(text.contains("clientservice"), "No se deben exponer claves de sesion Service");
            assertFalse(text.contains("contrasenia"), "No se deben exponer secretos demo");
            assertFalse(text.contains("contraseña"), "No se deben exponer secretos demo");
        }
    }

    private static GatewayAuthClient newSlowGatewayClient() {
        return new GatewayAuthClient() {
            @Override
            public String configuredClientId() {
                return "1";
            }

            @Override
            public com.portfolio.auth.transport.secure.SecureAsResponse requestTicketGrantingTicket(String requestId)
                    throws Exception {
                Thread.sleep(1_000);
                throw new IOException("slow flow interrupted");
            }

            @Override
            public com.portfolio.auth.transport.secure.SecureTgsResponse requestServiceTicket(
                    com.portfolio.auth.transport.secure.SecureAsResponse asResponse,
                    String serviceId,
                    String requestId,
                    String authenticatorId,
                    Instant authenticatorIssuedAt) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public com.portfolio.auth.core.protocol.dto.ServiceResponse requestProtectedService(
                    com.portfolio.auth.transport.secure.SecureTgsResponse tgsResponse,
                    String requestId,
                    String authenticatorId,
                    Instant authenticatorIssuedAt) {
                throw new UnsupportedOperationException("not used");
            }
        };
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

    private static final class GatewayServer implements AutoCloseable {
        private final AuthWebSocketServer server;

        private GatewayServer(AuthWebSocketServer server) {
            this.server = server;
        }

        private static GatewayServer start(AuthClient client) throws IOException {
            return start(client, WebSocketGatewayPolicy.defaults());
        }

        private static GatewayServer start(AuthClient client, WebSocketGatewayPolicy policy) throws IOException {
            AuthConfig config = AuthConfig.localDemo();
            return start(new DefaultGatewayAuthClient(config, client), policy);
        }

        private static GatewayServer start(GatewayAuthClient client, WebSocketGatewayPolicy policy) throws IOException {
            int port = freePort();
            AuthConfig config = AuthConfig.localDemo();
            GatewaySessionService sessionService = GatewaySessionService.inMemory(config);
            GatewayAuthFlowService flowService = new GatewayAuthFlowService(
                    config,
                    client,
                    NoOpAuthEventRepository.INSTANCE,
                    sessionService);
            AuthWebSocketServer server = new AuthWebSocketServer(
                    new InetSocketAddress(AuthConfig.DEFAULT_LOCAL_HOST, port),
                    new WebSocketMessageCodec(),
                    new WebSocketMessageProcessor(flowService, sessionService),
                    policy);
            server.start();
            return new GatewayServer(server);
        }

        private static GatewayServer startUnavailable() throws IOException {
            return startUnavailable(WebSocketGatewayPolicy.defaults());
        }

        private static GatewayServer startUnavailable(WebSocketGatewayPolicy policy) throws IOException {
            AuthConfig config = AuthConfig.localDemo();
            AuthClient client = new AuthClient(
                    config,
                    AuthConfig.DEFAULT_LOCAL_HOST,
                    freePort(),
                    freePort(),
                    freePort());
            return start(client, policy);
        }

        private int port() {
            return server.getPort();
        }

        @Override
        public void close() {
            server.shutdown();
        }
    }

    private static final class RecordingWebSocketClient extends WebSocketClient implements AutoCloseable {
        private final WebSocketMessageCodec codec;
        private final CountDownLatch openLatch = new CountDownLatch(1);
        private final BlockingQueue<WebSocketMessage> messages = new LinkedBlockingQueue<>();
        private final AtomicReference<Exception> error = new AtomicReference<>();

        private RecordingWebSocketClient(URI serverUri, WebSocketMessageCodec codec) {
            super(serverUri);
            this.codec = codec;
        }

        private RecordingWebSocketClient(URI serverUri, WebSocketMessageCodec codec, Map<String, String> headers) {
            super(serverUri, headers);
            this.codec = codec;
        }

        private static RecordingWebSocketClient connect(int port, WebSocketMessageCodec codec) throws Exception {
            return connect(port, codec, Map.of());
        }

        private static RecordingWebSocketClient connect(
                int port,
                WebSocketMessageCodec codec,
                Map<String, String> headers) throws Exception {
            URI uri = URI.create("ws://" + AuthConfig.DEFAULT_LOCAL_HOST + ":" + port);
            long deadline = System.nanoTime() + TIMEOUT.toNanos();
            Exception lastError = null;

            while (System.nanoTime() < deadline) {
                RecordingWebSocketClient client = new RecordingWebSocketClient(uri, codec, headers);
                try {
                    if (client.connectBlocking(250, TimeUnit.MILLISECONDS)
                            && client.openLatch.await(250, TimeUnit.MILLISECONDS)) {
                        return client;
                    }
                } catch (Exception e) {
                    lastError = e;
                }
                client.close();
                Thread.sleep(25);
            }

            if (lastError != null) {
                fail("No se pudo conectar al gateway WebSocket: " + lastError.getMessage());
            }
            fail("No se pudo conectar al gateway WebSocket en " + TIMEOUT.toMillis() + " ms");
            throw new IllegalStateException("unreachable");
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            openLatch.countDown();
        }

        @Override
        public void onMessage(String message) {
            messages.add(codec.decode(message));
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            // Tests close clients explicitly after receiving the terminal message.
        }

        @Override
        public void onError(Exception exception) {
            error.set(exception);
        }

        private WebSocketMessage awaitType(WebSocketMessageType type) throws InterruptedException {
            List<WebSocketMessage> received = awaitUntil(type);
            return received.get(received.size() - 1);
        }

        private List<WebSocketMessage> awaitUntil(WebSocketMessageType terminalType) throws InterruptedException {
            long deadline = System.nanoTime() + TIMEOUT.toNanos();
            List<WebSocketMessage> received = new ArrayList<>();
            while (System.nanoTime() < deadline) {
                long remainingMillis = Math.max(1, (deadline - System.nanoTime()) / 1_000_000);
                WebSocketMessage message = messages.poll(remainingMillis, TimeUnit.MILLISECONDS);
                if (message == null) {
                    break;
                }
                received.add(message);
                if (message.type() == terminalType) {
                    return received;
                }
            }
            Exception lastError = error.get();
            if (lastError != null) {
                fail("No se recibio " + terminalType + "; ultimo error WebSocket: " + lastError.getMessage());
            }
            fail("No se recibio " + terminalType + ". Mensajes recibidos: " + received);
            return received;
        }

    }
}
