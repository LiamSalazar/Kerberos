package com.portfolio.auth.service;

import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.ClientAuthenticator;
import com.portfolio.auth.core.protocol.dto.ServiceResponse;
import com.portfolio.auth.core.protocol.dto.TicketService;
import com.portfolio.auth.core.repository.InMemoryServiceRegistry;
import com.portfolio.auth.core.replay.InMemoryReplayCache;
import com.portfolio.auth.crypto.AesGcmCryptoService;
import com.portfolio.auth.crypto.CryptoEnvelope;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.protocol.MessageType;
import com.portfolio.auth.transport.protocol.ProtocolEnvelope;
import com.portfolio.auth.transport.secure.SecureAad;
import com.portfolio.auth.transport.secure.SecureJsonCrypto;
import com.portfolio.auth.transport.secure.SecureServiceRequest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpProtectedResourceTest {
    @Test
    void shouldCallLocalHttpResourceWithoutExposingSecrets() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext("/protected", exchange -> {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(requestBody.contains("\"requestId\":\"req-http\""));
                assertTrue(requestBody.contains("\"clientId\":\"client-http\""));
                assertTrue(requestBody.contains("\"serviceId\":\"service-http\""));
                byte[] response = "Protected HTTP resource granted".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();

            HttpProtectedResource resource = new HttpProtectedResource(
                    "service-http",
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/protected"));

            ProtectedServiceResponse response = resource.execute(new ProtectedServiceRequest(
                    "req-http",
                    "client-http",
                    "service-http",
                    Instant.now()));

            assertTrue(response.accessGranted());
            assertEquals("Protected HTTP resource granted", response.serviceMessage());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldInvokeHttpResourceOnlyAfterServiceValidationPasses() throws Exception {
        AtomicInteger invocations = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext("/protected", exchange -> {
                invocations.incrementAndGet();
                byte[] response = "handler granted".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();

            AuthConfig config = AuthConfig.localDemo();
            JsonMessageCodec codec = new JsonMessageCodec();
            SecureJsonCrypto secureJsonCrypto = new SecureJsonCrypto(
                    codec,
                    new AesGcmCryptoService(),
                    config.demoPbkdf2Salt());
            ProtectedServiceHandler handler = new ProtectedServiceHandler(
                    config,
                    InMemoryServiceRegistry.fromConfig(config),
                    new HttpProtectedResource(
                            config.defaultServiceId(),
                            URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/protected")),
                    new InMemoryReplayCache(),
                    codec,
                    secureJsonCrypto);

            SecureServiceRequest validRequest = serviceRequest(config, codec, secureJsonCrypto, "svc-valid", "1");
            ProtocolEnvelope validResponse = handler.handle(envelope(codec, validRequest));

            assertEquals(MessageType.SERVICE_RESPONSE, validResponse.messageType());
            ServiceResponse decrypted = secureJsonCrypto.decrypt(
                    codec.decodePayload(validResponse.payloadJson(), CryptoEnvelope.class),
                    "client-service-session",
                    SecureAad.serviceResponse("svc-valid"),
                    ServiceResponse.class);
            assertTrue(decrypted.accessGranted());
            assertEquals("handler granted", decrypted.serviceMessage());
            assertEquals(1, invocations.get());

            SecureServiceRequest invalidRequest = serviceRequest(config, codec, secureJsonCrypto, "svc-invalid", "wrong-client");
            ProtocolEnvelope invalidResponse = handler.handle(envelope(codec, invalidRequest));

            assertEquals(MessageType.ERROR_RESPONSE, invalidResponse.messageType());
            assertEquals(1, invocations.get());
        } finally {
            server.stop(0);
        }
    }

    private static SecureServiceRequest serviceRequest(
            AuthConfig config,
            JsonMessageCodec codec,
            SecureJsonCrypto secureJsonCrypto,
            String requestId,
            String authenticatorClientId) throws Exception {
        Instant now = Instant.now();
        TicketService ticket = new TicketService(
                ProtocolDefaults.CURRENT_VERSION,
                "ticket-" + requestId,
                now,
                now.plusSeconds(60),
                config.defaultClientId(),
                config.clientHost(),
                config.defaultServiceId(),
                "client-service-session");
        ClientAuthenticator authenticator = new ClientAuthenticator(
                ProtocolDefaults.CURRENT_VERSION,
                "auth-" + requestId,
                now,
                now.plusSeconds(60),
                authenticatorClientId,
                config.clientHost());
        return new SecureServiceRequest(
                ProtocolDefaults.CURRENT_VERSION,
                requestId,
                now,
                config.defaultClientId(),
                config.defaultServiceId(),
                secureJsonCrypto.encrypt(ticket, config.demoServiceSecret(), SecureAad.ticketService()),
                secureJsonCrypto.encrypt(authenticator, "client-service-session", SecureAad.authenticator(requestId)));
    }

    private static ProtocolEnvelope envelope(JsonMessageCodec codec, SecureServiceRequest request) {
        return new ProtocolEnvelope(
                MessageType.SERVICE_REQUEST,
                ProtocolDefaults.CURRENT_VERSION,
                request.requestId(),
                request.issuedAt(),
                codec.encodePayload(request));
    }
}
