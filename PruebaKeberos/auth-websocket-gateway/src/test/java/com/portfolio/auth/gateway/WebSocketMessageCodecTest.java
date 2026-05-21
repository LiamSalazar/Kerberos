package com.portfolio.auth.gateway;

import org.junit.jupiter.api.Test;

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
        WebSocketMessage result = WebSocketMessage.flowResult("ws-1", true, "ok", 1, 2, 3, 6);

        String json = codec.encode(result);

        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"asMillis\":1"));
        assertTrue(json.contains("\"totalMillis\":6"));
    }

    @Test
    void shouldRoundTripOutputContractFields() {
        WebSocketMessage original = WebSocketMessage.flowResult(
                new WebSocketFlowResult("ws-result", true, "service granted", 10, 20, 30, 60));

        WebSocketMessage decoded = codec.decode(codec.encode(original));

        assertEquals(WebSocketMessageType.FLOW_RESULT, decoded.type());
        assertEquals("ws-result", decoded.requestId());
        assertEquals(true, decoded.success());
        assertEquals("service granted", decoded.serviceMessage());
        assertEquals(10L, decoded.asMillis());
        assertEquals(60L, decoded.totalMillis());
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
