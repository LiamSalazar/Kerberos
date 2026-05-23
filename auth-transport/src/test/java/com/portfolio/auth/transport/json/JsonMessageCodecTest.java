package com.portfolio.auth.transport.json;

import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.AsRequest;
import com.portfolio.auth.crypto.CryptoEnvelope;
import com.portfolio.auth.transport.protocol.MessageType;
import com.portfolio.auth.transport.protocol.ProtocolEnvelope;
import com.portfolio.auth.transport.secure.SecureAsResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonMessageCodecTest {
    private final JsonMessageCodec codec = new JsonMessageCodec();

    @Test
    void shouldRoundTripProtocolEnvelopeWithDtoPayload() {
        AsRequest request = new AsRequest(
                ProtocolDefaults.CURRENT_VERSION,
                "req-1",
                "client-1",
                "tgs-1",
                Instant.parse("2026-05-18T10:00:00Z"));
        ProtocolEnvelope envelope = new ProtocolEnvelope(
                MessageType.AS_REQUEST,
                ProtocolDefaults.CURRENT_VERSION,
                request.requestId(),
                request.issuedAt(),
                codec.encodePayload(request));

        ProtocolEnvelope decodedEnvelope = codec.decodeEnvelope(codec.encode(envelope));
        AsRequest decodedPayload = codec.decodePayload(decodedEnvelope.payloadJson(), AsRequest.class);

        assertEquals(envelope.messageType(), decodedEnvelope.messageType());
        assertEquals(request, decodedPayload);
    }

    @Test
    void shouldRoundTripCryptoEnvelopeInsideSecurePayload() {
        CryptoEnvelope ticketEnvelope = new CryptoEnvelope(
                "crypto-envelope/1",
                "AES/GCM/NoPadding",
                new byte[] { 1, 2, 3 },
                new byte[] { 4, 5, 6 },
                Instant.parse("2026-05-18T10:00:01Z"));
        SecureAsResponse response = new SecureAsResponse(
                ProtocolDefaults.CURRENT_VERSION,
                "req-1",
                Instant.parse("2026-05-18T10:00:00Z"),
                Instant.parse("2026-05-18T10:05:00Z"),
                "client-1",
                "tgs-1",
                "session-key",
                ticketEnvelope);

        SecureAsResponse decoded = codec.decodePayload(codec.encodePayload(response), SecureAsResponse.class);

        assertEquals(response.clientTgsSessionKey(), decoded.clientTgsSessionKey());
        assertArrayEquals(ticketEnvelope.iv(), decoded.ticketTgsEnvelope().iv());
        assertArrayEquals(ticketEnvelope.ciphertext(), decoded.ticketTgsEnvelope().ciphertext());
    }

    @Test
    void shouldHandleEscapedStringsAndExtraFields() {
        String json = """
                {
                  "messageType": "AS_REQUEST",
                  "version": "auth-protocol/1",
                  "requestId": "req-extra",
                  "issuedAt": "2026-05-18T10:00:00Z",
                  "extra": "ignored",
                  "payload": {
                    "version": "auth-protocol/1",
                    "requestId": "req-extra",
                    "clientId": "client-1",
                    "ticketGrantingServerId": "tgs-1",
                    "issuedAt": "2026-05-18T10:00:00Z",
                    "note": "line\\nquote\\\""
                  }
                }
                """;

        ProtocolEnvelope decodedEnvelope = codec.decodeEnvelope(json);
        AsRequest decodedPayload = codec.decodePayload(decodedEnvelope.payloadJson(), AsRequest.class);

        assertEquals(MessageType.AS_REQUEST, decodedEnvelope.messageType());
        assertEquals("client-1", decodedPayload.clientId());
    }

    @Test
    void shouldRejectMissingPayload() {
        String json = """
                {
                  "messageType": "AS_REQUEST",
                  "version": "auth-protocol/1",
                  "requestId": "req-missing",
                  "issuedAt": "2026-05-18T10:00:00Z"
                }
                """;

        assertThrows(IllegalArgumentException.class, () -> codec.decodeEnvelope(json));
    }

    @Test
    void shouldRejectWrongEnvelopeFieldTypes() {
        String json = """
                {
                  "messageType": "AS_REQUEST",
                  "version": 1,
                  "requestId": "req-wrong",
                  "issuedAt": "2026-05-18T10:00:00Z",
                  "payload": {}
                }
                """;

        assertThrows(IllegalArgumentException.class, () -> codec.decodeEnvelope(json));
    }

    @Test
    void shouldRejectUnsupportedMessageType() {
        String json = """
                {
                  "messageType": "BAD_REQUEST",
                  "version": "auth-protocol/1",
                  "requestId": "req-bad",
                  "issuedAt": "2026-05-18T10:00:00Z",
                  "payload": {}
                }
                """;

        assertThrows(IllegalArgumentException.class, () -> codec.decodeEnvelope(json));
    }

    @Test
    void shouldRejectEmptyMalformedAndTruncatedJson() {
        assertThrows(IllegalArgumentException.class, () -> codec.decodeEnvelope(""));
        assertThrows(IllegalArgumentException.class, () -> codec.decodeEnvelope("{"));
        assertThrows(IllegalArgumentException.class, () -> codec.decodeEnvelope("{\"messageType\":\"AS_REQUEST\""));
    }

    @Test
    void shouldRejectInvalidPayloadForExpectedDto() {
        String payload = """
                {
                  "version": "auth-protocol/1",
                  "requestId": "req-payload",
                  "clientId": 1,
                  "ticketGrantingServerId": "tgs-1",
                  "issuedAt": "2026-05-18T10:00:00Z"
                }
                """;

        assertThrows(IllegalArgumentException.class, () -> codec.decodePayload(payload, AsRequest.class));
    }

    @Test
    void shouldRejectInvalidProtocolEnvelopeFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProtocolEnvelope(
                        MessageType.AS_REQUEST,
                        ProtocolDefaults.CURRENT_VERSION,
                        " ",
                        Instant.parse("2026-05-18T10:00:00Z"),
                        "{}"));
    }
}
