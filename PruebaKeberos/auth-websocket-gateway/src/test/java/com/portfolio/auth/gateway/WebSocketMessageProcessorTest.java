package com.portfolio.auth.gateway;

import com.portfolio.auth.core.config.AuthConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WebSocketMessageProcessorTest {

    @Test
    void shouldReplyPong() {
        WebSocketMessageProcessor processor = new WebSocketMessageProcessor(newFlowService());

        WebSocketMessage output = processor.process(
                WebSocketMessage.inbound(WebSocketMessageType.PING, "ping-1", null, null),
                ignored -> { });

        assertEquals(WebSocketMessageType.PONG, output.type());
        assertEquals("ping-1", output.requestId());
    }

    @Test
    void shouldReturnErrorForOutputOnlyMessageType() {
        WebSocketMessageProcessor processor = new WebSocketMessageProcessor(newFlowService());

        WebSocketMessage output = processor.process(WebSocketMessage.ready(), ignored -> { });

        assertEquals(WebSocketMessageType.ERROR, output.type());
        assertFalse(output.success());
    }

    @Test
    void shouldProcessValidStartAuthFlow() {
        WebSocketMessageProcessor processor = new WebSocketMessageProcessor(newFlowService());
        List<WebSocketMessage> events = new ArrayList<>();

        WebSocketMessage output = processor.process(
                WebSocketMessage.inbound(WebSocketMessageType.START_AUTH_FLOW, "ws-processor", "2", "1"),
                events::add);

        assertEquals(WebSocketMessageType.FLOW_RESULT, output.type());
        assertFalse(output.success());
        assertEquals(2, events.size());
    }

    private static GatewayAuthFlowService newFlowService() {
        GatewayAuthClient client = new GatewayAuthClient() {
            @Override
            public String configuredClientId() {
                return "1";
            }

            @Override
            public com.portfolio.auth.transport.secure.SecureAsResponse requestTicketGrantingTicket(String requestId) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public com.portfolio.auth.transport.secure.SecureTgsResponse requestServiceTicket(
                    com.portfolio.auth.transport.secure.SecureAsResponse asResponse,
                    String serviceId,
                    String requestId,
                    String authenticatorId,
                    java.time.Instant authenticatorIssuedAt) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public com.portfolio.auth.core.protocol.dto.ServiceResponse requestProtectedService(
                    com.portfolio.auth.transport.secure.SecureTgsResponse tgsResponse,
                    String requestId,
                    String authenticatorId,
                    java.time.Instant authenticatorIssuedAt) {
                throw new UnsupportedOperationException("not used");
            }
        };
        return new GatewayAuthFlowService(AuthConfig.localDemo(), client);
    }
}
