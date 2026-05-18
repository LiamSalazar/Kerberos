package com.portfolio.auth.transport.legacy;

import com.portfolio.auth.core.protocol.dto.ClientAuthenticator;
import com.portfolio.auth.core.protocol.dto.TgsRequest;
import com.portfolio.auth.core.protocol.dto.TicketTgs;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class LegacyTgsRequestMapper {
    public static final String SERVICE_ID_KEY = "[Id-v]";
    public static final String TICKET_TGS_KEY = "[Ticket-tgs]";
    public static final String CLIENT_AUTHENTICATOR_KEY = "[Autentificador-c]";

    private LegacyTgsRequestMapper() {
    }

    public static HashMap<String, Object> toLegacyMap(TgsRequest request) {
        return toLegacyMap(request, request.ticketTgs(), request.clientAuthenticator());
    }

    public static HashMap<String, Object> toLegacyMap(
            TgsRequest request,
            Object legacyTicketTgs,
            Object legacyClientAuthenticator) {
        Objects.requireNonNull(request, "request");

        HashMap<String, Object> legacy = new HashMap<>();
        legacy.put(SERVICE_ID_KEY, request.serviceId());
        legacy.put(TICKET_TGS_KEY, legacyTicketTgs);
        legacy.put(CLIENT_AUTHENTICATOR_KEY, legacyClientAuthenticator);
        legacy.put(LegacyMapperSupport.CLIENT_ID_KEY, request.clientId());
        legacy.put(LegacyMapperSupport.TGS_ID_KEY, request.ticketGrantingServerId());
        legacy.put(LegacyMapperSupport.VERSION_KEY, request.version());
        legacy.put(LegacyMapperSupport.REQUEST_ID_KEY, request.requestId());
        legacy.put(LegacyMapperSupport.ISSUED_AT_KEY, request.issuedAt());
        return legacy;
    }

    public static TgsRequest fromLegacyMap(Map<String, Object> legacyPayload) {
        Objects.requireNonNull(legacyPayload, "legacyPayload");

        TicketTgs ticketTgs = LegacyMapperSupport.typedOrNull(legacyPayload, TICKET_TGS_KEY, TicketTgs.class);
        ClientAuthenticator authenticator = LegacyMapperSupport.typedOrNull(
                legacyPayload,
                CLIENT_AUTHENTICATOR_KEY,
                ClientAuthenticator.class);

        String clientId = LegacyMapperSupport.stringOrNull(legacyPayload, LegacyMapperSupport.CLIENT_ID_KEY);
        if (clientId == null && authenticator != null) {
            clientId = authenticator.clientId();
        }

        String tgsId = LegacyMapperSupport.stringOrNull(legacyPayload, LegacyMapperSupport.TGS_ID_KEY);
        if (tgsId == null && ticketTgs != null) {
            tgsId = ticketTgs.ticketGrantingServerId();
        }

        Instant issuedAt = LegacyMapperSupport.instantOrNull(legacyPayload, LegacyMapperSupport.ISSUED_AT_KEY);
        if (issuedAt == null && authenticator != null) {
            issuedAt = authenticator.issuedAt();
        }

        return new TgsRequest(
                LegacyMapperSupport.version(legacyPayload),
                LegacyMapperSupport.requestId(legacyPayload),
                requireIssuedAt(issuedAt),
                clientId,
                LegacyMapperSupport.requireString(legacyPayload, SERVICE_ID_KEY),
                tgsId,
                ticketTgs,
                authenticator);
    }

    private static Instant requireIssuedAt(Instant issuedAt) {
        if (issuedAt == null) {
            throw new IllegalArgumentException("Falta el campo temporal " + LegacyMapperSupport.ISSUED_AT_KEY);
        }
        return issuedAt;
    }
}
