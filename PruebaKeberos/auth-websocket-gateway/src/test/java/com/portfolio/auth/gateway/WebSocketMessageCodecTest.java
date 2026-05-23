package com.portfolio.auth.gateway;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketMessageCodecTest {
    private final WebSocketMessageCodec codec = new WebSocketMessageCodec();

    @Test
    void shouldDecodeStartAuthFlowInput() {
        String json = """
                {
                  "type": "START_AUTH_FLOW",
                  "requestId": "ws-1",
                  "clientId": "1",
                  "serviceId": "1",
                  "extra": "ignored"
                }
                """;

        WebSocketMessage message = codec.decode(json);

        assertEquals(WebSocketMessageType.START_AUTH_FLOW, message.type());
        assertEquals("ws-1", message.requestId());
        assertEquals("1", message.clientId());
        assertEquals("1", message.serviceId());
    }

    @Test
    void shouldEncodeFlowEventWithEscapedText() {
        WebSocketMessage event = WebSocketMessage.flowEvent(
                "ws-1",
                WebSocketFlowStage.AS_RESPONSE_RECEIVED,
                "TGT emitido \"ok\"");

        String json = codec.encode(event);

        assertTrue(json.contains("\"type\":\"FLOW_EVENT\""));
        assertTrue(json.contains("\"stage\":\"AS_RESPONSE_RECEIVED\""));
        assertTrue(json.contains("TGT emitido \\\"ok\\\""));
    }

    @Test
    void shouldEncodeFlowResultTimings() {
        WebSocketMessage result = WebSocketMessage.flowResult(
                "ws-1",
                true,
                null,
                "ok",
                "session-1",
                Instant.parse("2026-05-23T00:05:00Z"),
                1,
                2,
                3,
                6);

        String json = codec.encode(result);

        assertTrue(json.contains("\"success\":true"));
        assertTrue(!json.contains("\"errorType\""));
        assertTrue(json.contains("\"sessionId\":\"session-1\""));
        assertTrue(json.contains("\"sessionExpiresAt\":\"2026-05-23T00:05:00Z\""));
        assertTrue(json.contains("\"asMillis\":1"));
        assertTrue(json.contains("\"totalMillis\":6"));
    }

    @Test
    void shouldEncodeErrorTypeForFailedFlow() {
        WebSocketMessage result = WebSocketMessage.flowResult(
                "ws-1",
                false,
                WebSocketErrorType.SERVICE_NOT_FOUND,
                "service missing",
                null,
                null,
                1,
                2,
                0,
                3);

        String json = codec.encode(result);

        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("\"errorType\":\"SERVICE_NOT_FOUND\""));
    }

    @Test
    void shouldRoundTripOutputContractFields() {
        WebSocketMessage original = WebSocketMessage.flowResult(
                new WebSocketFlowResult(
                        "ws-result",
                        true,
                        null,
                        "service granted",
                        "session-1",
                        Instant.parse("2026-05-23T00:05:00Z"),
                        10,
                        20,
                        30,
                        60));

        WebSocketMessage decoded = codec.decode(codec.encode(original));

        assertEquals(WebSocketMessageType.FLOW_RESULT, decoded.type());
        assertEquals("ws-result", decoded.requestId());
        assertEquals(true, decoded.success());
        assertEquals(null, decoded.errorType());
        assertEquals("service granted", decoded.serviceMessage());
        assertEquals("session-1", decoded.sessionId());
        assertEquals("2026-05-23T00:05:00Z", decoded.sessionExpiresAt());
        assertEquals(10L, decoded.asMillis());
        assertEquals(60L, decoded.totalMillis());
    }

    @Test
    void shouldDecodeVerifySessionInput() {
        WebSocketMessage message = codec.decode("""
                {
                  "type": "VERIFY_SESSION",
                  "requestId": "verify-1",
                  "sessionId": "opaque-session",
                  "clientId": "1",
                  "serviceId": "1"
                }
                """);

        assertEquals(WebSocketMessageType.VERIFY_SESSION, message.type());
        assertEquals("opaque-session", message.sessionId());
        assertEquals("1", message.clientId());
        assertEquals("1", message.serviceId());
    }

    @Test
    void shouldEncodeSessionInvalid() {
        WebSocketMessage message = WebSocketMessage.sessionInvalid(
                "verify-1",
                com.portfolio.auth.core.session.SessionValidationStatus.EXPIRED);

        String json = codec.encode(message);

        assertTrue(json.contains("\"type\":\"SESSION_INVALID\""));
        assertTrue(json.contains("\"valid\":false"));
        assertTrue(json.contains("\"reason\":\"EXPIRED\""));
    }

    @Test
    void shouldRejectMalformedJson() {
        assertThrows(IllegalArgumentException.class, () -> codec.decode("{"));
        assertThrows(IllegalArgumentException.class, () -> codec.decode(""));
    }

    @Test
    void shouldRejectUnknownTypeAndWrongFieldTypes() {
        assertThrows(IllegalArgumentException.class, () -> codec.decode("{\"type\":\"NOPE\"}"));
        assertThrows(IllegalArgumentException.class, () -> codec.decode("{\"type\":1}"));
        assertThrows(IllegalArgumentException.class,
                () -> codec.decode("{\"type\":\"START_AUTH_FLOW\",\"requestId\":1}"));
    }
}
