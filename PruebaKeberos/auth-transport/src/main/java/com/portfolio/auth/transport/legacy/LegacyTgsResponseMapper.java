package com.portfolio.auth.transport.legacy;

import com.portfolio.auth.core.protocol.dto.TgsResponse;
import com.portfolio.auth.core.protocol.dto.TicketService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class LegacyTgsResponseMapper {
    public static final String CLIENT_SERVICE_SESSION_KEY = "[K-c_v]";
    public static final String SERVICE_ID_KEY = "[Id-v]";
    public static final String TIMESTAMP_KEY = "[TimeStamp-4]";
    public static final String LIFETIME_KEY = "[TiempoVida-4]";
    public static final String TICKET_SERVICE_KEY = "[Ticket-v]";

    private LegacyTgsResponseMapper() {
    }

    public static HashMap<String, Object> toLegacyMap(TgsResponse response) {
        return toLegacyMap(response, response.ticketService());
    }

    public static HashMap<String, Object> toLegacyMap(TgsResponse response, Object legacyTicketService) {
        Objects.requireNonNull(response, "response");

        HashMap<String, Object> legacy = new HashMap<>();
        legacy.put(CLIENT_SERVICE_SESSION_KEY, response.clientServiceSessionKey());
        legacy.put(SERVICE_ID_KEY, response.serviceId());
        legacy.put(TIMESTAMP_KEY, LegacyMapperSupport.toLegacyDateTime(response.issuedAt()));
        legacy.put(LIFETIME_KEY, LegacyMapperSupport.toLegacyDateTime(response.expiresAt()));
        legacy.put(TICKET_SERVICE_KEY, legacyTicketService);
        legacy.put(LegacyMapperSupport.CLIENT_ID_KEY, response.clientId());
        legacy.put(LegacyMapperSupport.VERSION_KEY, response.version());
        legacy.put(LegacyMapperSupport.REQUEST_ID_KEY, response.requestId());
        legacy.put(LegacyMapperSupport.ISSUED_AT_KEY, response.issuedAt());
        legacy.put(LegacyMapperSupport.EXPIRES_AT_KEY, response.expiresAt());
        return legacy;
    }

    public static TgsResponse fromLegacyMap(Map<String, Object> legacyPayload) {
        Objects.requireNonNull(legacyPayload, "legacyPayload");

        return new TgsResponse(
                LegacyMapperSupport.version(legacyPayload),
                LegacyMapperSupport.requestId(legacyPayload),
                LegacyMapperSupport.instantFromAny(legacyPayload, LegacyMapperSupport.ISSUED_AT_KEY, TIMESTAMP_KEY),
                LegacyMapperSupport.instantFromAny(legacyPayload, LegacyMapperSupport.EXPIRES_AT_KEY, LIFETIME_KEY),
                LegacyMapperSupport.stringOrNull(legacyPayload, LegacyMapperSupport.CLIENT_ID_KEY),
                LegacyMapperSupport.requireString(legacyPayload, SERVICE_ID_KEY),
                LegacyMapperSupport.requireString(legacyPayload, CLIENT_SERVICE_SESSION_KEY),
                LegacyMapperSupport.typedOrNull(legacyPayload, TICKET_SERVICE_KEY, TicketService.class));
    }
}
