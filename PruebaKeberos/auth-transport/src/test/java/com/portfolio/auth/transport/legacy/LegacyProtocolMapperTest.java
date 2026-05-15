package com.portfolio.auth.transport.legacy;

import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.AsResponse;
import com.portfolio.auth.core.protocol.dto.ClientAuthenticator;
import com.portfolio.auth.core.protocol.dto.ServiceRequest;
import com.portfolio.auth.core.protocol.dto.ServiceResponse;
import com.portfolio.auth.core.protocol.dto.TgsRequest;
import com.portfolio.auth.core.protocol.dto.TgsResponse;
import com.portfolio.auth.core.protocol.dto.TicketService;
import com.portfolio.auth.core.protocol.dto.TicketTgs;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyProtocolMapperTest {
    private static final Instant ISSUED_AT = Instant.parse("2026-05-12T10:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-05-12T10:05:00Z");

    @Test
    void shouldMapAsResponseWithoutDroppingMainFields() {
        TicketTgs ticket = ticketTgs();
        AsResponse response = new AsResponse(
                ProtocolDefaults.CURRENT_VERSION,
                "as-res-1",
                ISSUED_AT,
                EXPIRES_AT,
                "client-1",
                "tgs-1",
                "k-client-tgs",
                ticket);

        HashMap<String, Object> legacy = LegacyAsResponseMapper.toLegacyMap(response);
        AsResponse mapped = LegacyAsResponseMapper.fromLegacyMap(legacy);

        assertEquals("k-client-tgs", legacy.get(LegacyAsResponseMapper.CLIENT_TGS_SESSION_KEY));
        assertEquals(ticket, legacy.get(LegacyAsResponseMapper.TICKET_TGS_KEY));
        assertEquals(response, mapped);
    }

    @Test
    void shouldMapTgsRequestWithoutDroppingMainFields() {
        TgsRequest request = new TgsRequest(
                ProtocolDefaults.CURRENT_VERSION,
                "tgs-req-1",
                ISSUED_AT,
                "client-1",
                "service-1",
                "tgs-1",
                ticketTgs(),
                authenticator());

        HashMap<String, Object> legacy = LegacyTgsRequestMapper.toLegacyMap(request);
        TgsRequest mapped = LegacyTgsRequestMapper.fromLegacyMap(legacy);

        assertEquals("service-1", legacy.get(LegacyTgsRequestMapper.SERVICE_ID_KEY));
        assertEquals(request.ticketTgs(), legacy.get(LegacyTgsRequestMapper.TICKET_TGS_KEY));
        assertEquals(request.clientAuthenticator(), legacy.get(LegacyTgsRequestMapper.CLIENT_AUTHENTICATOR_KEY));
        assertEquals(request, mapped);
    }

    @Test
    void shouldMapTgsResponseWithoutDroppingMainFields() {
        TgsResponse response = new TgsResponse(
                ProtocolDefaults.CURRENT_VERSION,
                "tgs-res-1",
                ISSUED_AT,
                EXPIRES_AT,
                "client-1",
                "service-1",
                "k-client-service",
                ticketService());

        HashMap<String, Object> legacy = LegacyTgsResponseMapper.toLegacyMap(response);
        TgsResponse mapped = LegacyTgsResponseMapper.fromLegacyMap(legacy);

        assertEquals("k-client-service", legacy.get(LegacyTgsResponseMapper.CLIENT_SERVICE_SESSION_KEY));
        assertEquals(response.ticketService(), legacy.get(LegacyTgsResponseMapper.TICKET_SERVICE_KEY));
        assertEquals(response, mapped);
    }

    @Test
    void shouldMapServiceRequestWithoutDroppingMainFields() {
        ServiceRequest request = new ServiceRequest(
                ProtocolDefaults.CURRENT_VERSION,
                "svc-req-1",
                ISSUED_AT,
                "client-1",
                "service-1",
                ticketService(),
                authenticator());

        HashMap<String, Object> legacy = LegacyServiceRequestMapper.toLegacyMap(request);
        ServiceRequest mapped = LegacyServiceRequestMapper.fromLegacyMap(legacy);

        assertEquals(request.ticketService(), legacy.get(LegacyServiceRequestMapper.TICKET_SERVICE_KEY));
        assertEquals(request.clientAuthenticator(), legacy.get(LegacyServiceRequestMapper.CLIENT_AUTHENTICATOR_KEY));
        assertEquals(request, mapped);
    }

    @Test
    void shouldMapServiceResponseWithoutDroppingMainFields() {
        ServiceResponse response = new ServiceResponse(
                ProtocolDefaults.CURRENT_VERSION,
                "svc-res-1",
                ISSUED_AT,
                EXPIRES_AT,
                "client-1",
                "service-1",
                ISSUED_AT,
                Instant.parse("2026-05-12T10:00:05Z"),
                "service granted",
                true);

        HashMap<String, Object> legacy = LegacyServiceResponseMapper.toLegacyMap(response);
        ServiceResponse mapped = LegacyServiceResponseMapper.fromLegacyMap(legacy);

        assertEquals("service granted", legacy.get(LegacyServiceResponseMapper.SERVICE_MESSAGE_KEY));
        assertEquals(true, legacy.get(LegacyServiceResponseMapper.ACCESS_GRANTED_KEY));
        assertEquals(response, mapped);
    }

    private static TicketTgs ticketTgs() {
        return new TicketTgs(
                ProtocolDefaults.CURRENT_VERSION,
                "ticket-tgs-1",
                ISSUED_AT,
                EXPIRES_AT,
                "client-1",
                "127.0.0.1",
                "tgs-1",
                "k-client-tgs");
    }

    private static TicketService ticketService() {
        return new TicketService(
                ProtocolDefaults.CURRENT_VERSION,
                "ticket-service-1",
                ISSUED_AT,
                EXPIRES_AT,
                "client-1",
                "127.0.0.1",
                "service-1",
                "k-client-service");
    }

    private static ClientAuthenticator authenticator() {
        return new ClientAuthenticator(
                ProtocolDefaults.CURRENT_VERSION,
                "auth-1",
                ISSUED_AT,
                EXPIRES_AT,
                "client-1",
                "127.0.0.1");
    }
}
