package com.portfolio.auth.transport.javaio;

import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.AsRequest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaObjectTransportTest {

    @Test
    void shouldRoundTripTypedAsRequest() throws Exception {
        AsRequest original = new AsRequest(
                ProtocolDefaults.CURRENT_VERSION,
                "req-123",
                "client-1",
                "tgs-1",
                Instant.parse("2026-04-20T10:15:30Z"));

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        JavaObjectTransport.send(buffer, original);

        AsRequest decoded = JavaObjectTransport.receive(new ByteArrayInputStream(buffer.toByteArray()), AsRequest.class);

        assertEquals(original, decoded);
    }
}
