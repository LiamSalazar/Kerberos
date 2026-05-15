package com.portfolio.auth.transport.legacy;

import com.portfolio.auth.core.protocol.dto.ClientAuthenticator;
import com.portfolio.auth.core.protocol.dto.ServiceRequest;
import com.portfolio.auth.core.protocol.dto.TicketService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class LegacyServiceRequestMapper {
    public static final String TICKET_SERVICE_KEY = "[Ticket-v]";
    public static final String CLIENT_AUTHENTICATOR_KEY = "[Autentificador-c]";

    private LegacyServiceRequestMapper() {
    }

    public static HashMap<String, Object> toLegacyMap(ServiceRequest request) {
        return toLegacyMap(request, request.ticketService(), request.clientAuthenticator());
    }

    public static HashMap<String, Object> toLegacyMap(
            ServiceRequest request,
            Object legacyTicketService,
            Object legacyClientAuthenticator) {
        Objects.requireNonNull(request, "request");

        HashMap<String, Object> legacy = new HashMap<>();
        legacy.put(TICKET_SERVICE_KEY, legacyTicketService);
        legacy.put(CLIENT_AUTHENTICATOR_KEY, legacyClientAuthenticator);
        legacy.put(LegacyMapperSupport.CLIENT_ID_KEY, request.clientId());
        legacy.put(LegacyMapperSupport.SERVICE_ID_KEY, request.serviceId());
        legacy.put(LegacyMapperSupport.VERSION_KEY, request.version());
        legacy.put(LegacyMapperSupport.REQUEST_ID_KEY, request.requestId());
        legacy.put(LegacyMapperSupport.ISSUED_AT_KEY, request.issuedAt());
        return legacy;
    }

    public static ServiceRequest fromLegacyMap(Map<String, Object> legacyPayload) {
        Objects.requireNonNull(legacyPayload, "legacyPayload");

        TicketService ticketService = LegacyMapperSupport.typedOrNull(
                legacyPayload,
                TICKET_SERVICE_KEY,
                TicketService.class);
        ClientAuthenticator authenticator = LegacyMapperSupport.typedOrNull(
                legacyPayload,
                CLIENT_AUTHENTICATOR_KEY,
                ClientAuthenticator.class);

        String clientId = LegacyMapperSupport.stringOrNull(legacyPayload, LegacyMapperSupport.CLIENT_ID_KEY);
        if (clientId == null && authenticator != null) {
            clientId = authenticator.clientId();
        }

        String serviceId = LegacyMapperSupport.stringOrNull(legacyPayload, LegacyMapperSupport.SERVICE_ID_KEY);
        if (serviceId == null && ticketService != null) {
            serviceId = ticketService.serviceId();
        }

        return new ServiceRequest(
                LegacyMapperSupport.version(legacyPayload),
                LegacyMapperSupport.requestId(legacyPayload),
                LegacyMapperSupport.instantFrom(legacyPayload.get(LegacyMapperSupport.ISSUED_AT_KEY),
                        LegacyMapperSupport.ISSUED_AT_KEY),
                clientId,
                serviceId,
                ticketService,
                authenticator);
    }
}
