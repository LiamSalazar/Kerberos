package com.portfolio.auth.gateway;

import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.session.AuthSession;
import com.portfolio.auth.core.session.InMemorySessionRepository;
import com.portfolio.auth.core.session.SessionValidationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WebSocketMessageProcessorTest {

    @Test
    void shouldReplyPong() {
        WebSocketMessageProcessor processor = newProcessor();

        WebSocketMessage output = processor.process(
                WebSocketMessage.inbound(WebSocketMessageType.PING, "ping-1", null, null),
                ignored -> { });

        assertEquals(WebSocketMessageType.PONG, output.type());
        assertEquals("ping-1", output.requestId());
    }

    @Test
    void shouldReturnErrorForOutputOnlyMessageType() {
        WebSocketMessageProcessor processor = newProcessor();

        WebSocketMessage output = processor.process(
                WebSocketMessage.flowEvent("ws-output", WebSocketFlowStage.FLOW_STARTED, "not input"),
                ignored -> { });

        assertEquals(WebSocketMessageType.ERROR, output.type());
        assertEquals(WebSocketErrorType.UNKNOWN_MESSAGE_TYPE.name(), output.errorType());
        assertFalse(output.success());
    }

    @Test
    void shouldProcessValidStartAuthFlow() {
        WebSocketMessageProcessor processor = newProcessor();
        List<WebSocketMessage> events = new ArrayList<>();

        WebSocketMessage output = processor.process(
                WebSocketMessage.inbound(WebSocketMessageType.START_AUTH_FLOW, "ws-processor", "2", "1"),
                events::add);

        assertEquals(WebSocketMessageType.FLOW_RESULT, output.type());
        assertFalse(output.success());
        assertEquals(2, events.size());
    }

    @Test
    void shouldRejectMissingStartAuthFlowFields() {
        WebSocketMessageProcessor processor = newProcessor();

        WebSocketMessage output = processor.process(
                WebSocketMessage.inbound(WebSocketMessageType.START_AUTH_FLOW, "ws-missing", "1", ""),
                ignored -> { });

        assertEquals(WebSocketMessageType.ERROR, output.type());
        assertEquals(WebSocketErrorType.MISSING_REQUIRED_FIELD.name(), output.errorType());
    }

    @Test
    void shouldVerifyAndLogoutSession() {
        InMemorySessionRepository repository = new InMemorySessionRepository();
        Instant now = Instant.now();
        repository.save(AuthSession.active(
                "session-1",
                "request-1",
                "client-1",
                "service-1",
                now,
                now.plusSeconds(60)));
        WebSocketMessageProcessor processor = new WebSocketMessageProcessor(
                newFlowService(),
                new GatewaySessionService(AuthConfig.localDemo(), repository));

        WebSocketMessage valid = processor.process(
                WebSocketMessage.sessionInbound(
                        WebSocketMessageType.VERIFY_SESSION,
                        "verify-1",
                        "session-1",
                        "client-1",
                        "service-1"),
                ignored -> { });

        assertEquals(WebSocketMessageType.SESSION_VALID, valid.type());
        assertEquals(true, valid.valid());
        assertEquals("client-1", valid.clientId());

        WebSocketMessage logout = processor.process(
                WebSocketMessage.sessionInbound(
                        WebSocketMessageType.LOGOUT_SESSION,
                        "logout-1",
                        "session-1",
                        null,
                        null),
                ignored -> { });

        assertEquals(WebSocketMessageType.SESSION_LOGGED_OUT, logout.type());

        WebSocketMessage revoked = processor.process(
                WebSocketMessage.sessionInbound(
                        WebSocketMessageType.VERIFY_SESSION,
                        "verify-2",
                        "session-1",
                        "client-1",
                        "service-1"),
                ignored -> { });

        assertEquals(WebSocketMessageType.SESSION_INVALID, revoked.type());
        assertEquals(SessionValidationStatus.REVOKED.name(), revoked.reason());
    }

    @Test
    void shouldRejectInvalidSessionRequest() {
        WebSocketMessageProcessor processor = newProcessor();

        WebSocketMessage missingSession = processor.process(
                WebSocketMessage.sessionInbound(
                        WebSocketMessageType.VERIFY_SESSION,
                        "verify-missing",
                        "",
                        "client-1",
                        "service-1"),
                ignored -> { });

        assertEquals(WebSocketMessageType.ERROR, missingSession.type());
        assertEquals(WebSocketErrorType.SESSION_REQUIRED.name(), missingSession.errorType());

        WebSocketMessage missingService = processor.process(
                WebSocketMessage.sessionInbound(
                        WebSocketMessageType.VERIFY_SESSION,
                        "verify-invalid",
                        "session-1",
                        "client-1",
                        ""),
                ignored -> { });

        assertEquals(WebSocketMessageType.ERROR, missingService.type());
        assertEquals(WebSocketErrorType.INVALID_SESSION_REQUEST.name(), missingService.errorType());
    }

    private static WebSocketMessageProcessor newProcessor() {
        AuthConfig config = AuthConfig.localDemo();
        GatewaySessionService sessions = GatewaySessionService.inMemory(config);
        return new WebSocketMessageProcessor(newFlowService(), sessions);
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
