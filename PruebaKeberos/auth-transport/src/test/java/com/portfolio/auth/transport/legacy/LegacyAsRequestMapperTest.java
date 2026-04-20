package com.portfolio.auth.transport.legacy;

import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.AsRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LegacyAsRequestMapperTest {

    @Test
    void shouldRoundTripAsRequestWithCompatibilityFields() {
        AsRequest original = new AsRequest(
                ProtocolDefaults.CURRENT_VERSION,
                "req-123",
                "client-1",
                "tgs-1",
                Instant.parse("2026-04-20T10:15:30Z"));

        HashMap<String, Object> legacyPayload = LegacyAsRequestMapper.toLegacyMap(original);
        AsRequest mapped = LegacyAsRequestMapper.fromLegacyMap(legacyPayload);

        assertEquals(original, mapped);
    }

    @Test
    void shouldMapPureLegacyPayloadEvenWithoutNewFields() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 4, 20, 10, 15, 30);

        HashMap<String, Object> legacyPayload = new HashMap<>();
        legacyPayload.put(LegacyAsRequestMapper.CLIENT_ID_KEY, "client-1");
        legacyPayload.put(LegacyAsRequestMapper.TGS_ID_KEY, "tgs-1");
        legacyPayload.put(LegacyAsRequestMapper.TIMESTAMP_KEY, timestamp);

        AsRequest mapped = LegacyAsRequestMapper.fromLegacyMap(legacyPayload);

        assertEquals("client-1", mapped.clientId());
        assertEquals("tgs-1", mapped.ticketGrantingServerId());
        assertEquals(timestamp.atZone(ZoneId.systemDefault()).toInstant(), mapped.issuedAt());
        assertEquals(ProtocolDefaults.CURRENT_VERSION, mapped.version());
        assertNotNull(mapped.requestId());
    }
}
