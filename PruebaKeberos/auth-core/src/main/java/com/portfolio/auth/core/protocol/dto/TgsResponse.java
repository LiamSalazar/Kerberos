package com.portfolio.auth.core.protocol.dto;

import java.time.Instant;

/**
 * Respuesta del Ticket Granting Server con la sesion cliente-servicio
 * y el ticket del servicio objetivo.
 */
public record TgsResponse(
        String version,
        String requestId,
        Instant issuedAt,
        Instant expiresAt,
        String clientId,
        String serviceId,
        String clientServiceSessionKey,
        TicketService ticketService
) {
}
