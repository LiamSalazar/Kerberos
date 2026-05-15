package com.portfolio.auth.transport.legacy;

import com.portfolio.auth.core.protocol.dto.AsResponse;
import com.portfolio.auth.core.protocol.dto.TicketTgs;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class LegacyAsResponseMapper {
    public static final String CLIENT_TGS_SESSION_KEY = "[K-c_tgs]";
    public static final String TGS_ID_KEY = "[Id-tgs]";
    public static final String TIMESTAMP_KEY = "[TimeStamp-2]";
    public static final String LIFETIME_KEY = "[TiempoVida-2]";
    public static final String TICKET_TGS_KEY = "[Ticket-tgs]";

    private LegacyAsResponseMapper() {
    }

    public static HashMap<String, Object> toLegacyMap(AsResponse response) {
        return toLegacyMap(response, response.ticketTgs());
    }

    public static HashMap<String, Object> toLegacyMap(AsResponse response, Object legacyTicketTgs) {
        Objects.requireNonNull(response, "response");

        HashMap<String, Object> legacy = new HashMap<>();
        legacy.put(CLIENT_TGS_SESSION_KEY, response.clientTgsSessionKey());
        legacy.put(TGS_ID_KEY, response.ticketGrantingServerId());
        legacy.put(TIMESTAMP_KEY, LegacyMapperSupport.toLegacyDateTime(response.issuedAt()));
        legacy.put(LIFETIME_KEY, LegacyMapperSupport.toLegacyDateTime(response.expiresAt()));
        legacy.put(TICKET_TGS_KEY, legacyTicketTgs);
        legacy.put(LegacyMapperSupport.CLIENT_ID_KEY, response.clientId());
        legacy.put(LegacyMapperSupport.VERSION_KEY, response.version());
        legacy.put(LegacyMapperSupport.REQUEST_ID_KEY, response.requestId());
        legacy.put(LegacyMapperSupport.ISSUED_AT_KEY, response.issuedAt());
        legacy.put(LegacyMapperSupport.EXPIRES_AT_KEY, response.expiresAt());
        return legacy;
    }

    public static AsResponse fromLegacyMap(Map<String, Object> legacyPayload) {
        Objects.requireNonNull(legacyPayload, "legacyPayload");

        return new AsResponse(
                LegacyMapperSupport.version(legacyPayload),
                LegacyMapperSupport.requestId(legacyPayload),
                LegacyMapperSupport.instantFromAny(legacyPayload, LegacyMapperSupport.ISSUED_AT_KEY, TIMESTAMP_KEY),
                LegacyMapperSupport.instantFromAny(legacyPayload, LegacyMapperSupport.EXPIRES_AT_KEY, LIFETIME_KEY),
                LegacyMapperSupport.stringOrNull(legacyPayload, LegacyMapperSupport.CLIENT_ID_KEY),
                LegacyMapperSupport.requireString(legacyPayload, TGS_ID_KEY),
                LegacyMapperSupport.requireString(legacyPayload, CLIENT_TGS_SESSION_KEY),
                LegacyMapperSupport.typedOrNull(legacyPayload, TICKET_TGS_KEY, TicketTgs.class));
    }
}
