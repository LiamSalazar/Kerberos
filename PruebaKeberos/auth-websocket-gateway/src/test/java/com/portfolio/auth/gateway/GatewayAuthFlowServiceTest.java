package com.portfolio.auth.gateway;

import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.ServiceResponse;
import com.portfolio.auth.crypto.CryptoEnvelope;
import com.portfolio.auth.transport.secure.SecureAsResponse;
import com.portfolio.auth.transport.secure.SecureTgsResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayAuthFlowServiceTest {

    @Test
    void shouldRunSuccessfulFlowAndPublishEvents() {
        FakeGatewayAuthClient client = new FakeGatewayAuthClient(false);
        GatewayAuthFlowService service = new GatewayAuthFlowService(AuthConfig.localDemo(), client);
        List<WebSocketMessage> events = new ArrayList<>();

        WebSocketMessage result = service.run(
                WebSocketMessage.inbound(WebSocketMessageType.START_AUTH_FLOW, "ws-ok", "1", "1"),
                events::add);

        assertEquals(WebSocketMessageType.FLOW_RESULT, result.type());
        assertTrue(result.success());
        assertEquals("MODULAR AUTH EXITOSO", result.serviceMessage());
        assertTrue(result.totalMillis() >= 0);
        assertEquals(1, client.asCalls);
        assertEquals(1, client.tgsCalls);
        assertEquals(1, client.serviceCalls);
        assertTrue(events.stream().anyMatch(event -> "AS_REQUEST_SENT".equals(event.stage())));
        assertTrue(events.stream().anyMatch(event -> "FLOW_SUCCESS".equals(event.stage())));
    }

    @Test
    void shouldRejectUnknownClientBeforeNetworkCalls() {
        FakeGatewayAuthClient client = new FakeGatewayAuthClient(false);
        GatewayAuthFlowService service = new GatewayAuthFlowService(AuthConfig.localDemo(), client);
        List<WebSocketMessage> events = new ArrayList<>();

        WebSocketMessage result = service.run(
                WebSocketMessage.inbound(WebSocketMessageType.START_AUTH_FLOW, "ws-client", "missing", "1"),
                events::add);

        assertFalse(result.success());
        assertEquals(0, client.asCalls);
        assertTrue(events.stream().anyMatch(event -> "FLOW_ERROR".equals(event.stage())));
    }

    @Test
    void shouldReturnControlledFailureWhenServicesAreUnavailable() {
        FakeGatewayAuthClient client = new FakeGatewayAuthClient(true);
        GatewayAuthFlowService service = new GatewayAuthFlowService(AuthConfig.localDemo(), client);
        List<WebSocketMessage> events = new ArrayList<>();

        WebSocketMessage result = service.run(
                WebSocketMessage.inbound(WebSocketMessageType.START_AUTH_FLOW, "ws-down", "1", "1"),
                events::add);

        assertFalse(result.success());
        assertTrue(result.serviceMessage().contains("AS no disponible"));
        assertTrue(events.stream().anyMatch(event -> "FLOW_ERROR".equals(event.stage())));
    }

    private static final class FakeGatewayAuthClient implements GatewayAuthClient {
        private final boolean failAtAs;
        private int asCalls;
        private int tgsCalls;
        private int serviceCalls;

        private FakeGatewayAuthClient(boolean failAtAs) {
            this.failAtAs = failAtAs;
        }

        @Override
        public String configuredClientId() {
            return "1";
        }

        @Override
        public SecureAsResponse requestTicketGrantingTicket(String requestId) throws Exception {
            asCalls++;
            if (failAtAs) {
                throw new IOException("AS no disponible");
            }
            Instant now = Instant.now();
            return new SecureAsResponse(
                    ProtocolDefaults.CURRENT_VERSION,
                    requestId,
                    now,
                    now.plusSeconds(60),
                    "1",
                    "1",
                    "client-tgs-session",
                    envelope());
        }

        @Override
        public SecureTgsResponse requestServiceTicket(
                SecureAsResponse asResponse,
                String serviceId,
                String requestId,
                String authenticatorId,
                Instant authenticatorIssuedAt) {
            tgsCalls++;
            Instant now = Instant.now();
            return new SecureTgsResponse(
                    ProtocolDefaults.CURRENT_VERSION,
                    requestId,
                    now,
                    now.plusSeconds(60),
                    "1",
                    serviceId,
                    "client-service-session",
                    envelope());
        }

        @Override
        public ServiceResponse requestProtectedService(
                SecureTgsResponse tgsResponse,
                String requestId,
                String authenticatorId,
                Instant authenticatorIssuedAt) {
            serviceCalls++;
            Instant now = Instant.now();
            return new ServiceResponse(
                    ProtocolDefaults.CURRENT_VERSION,
                    requestId,
                    now,
                    now.plusSeconds(60),
                    "1",
                    tgsResponse.serviceId(),
                    authenticatorIssuedAt,
                    now,
                    "MODULAR AUTH EXITOSO",
                    true);
        }

        private CryptoEnvelope envelope() {
            return new CryptoEnvelope(
                    "crypto-envelope/1",
                    "AES/GCM/NoPadding",
                    new byte[] { 1, 2, 3 },
                    new byte[] { 4, 5, 6 },
                    Instant.now());
        }
    }
}
