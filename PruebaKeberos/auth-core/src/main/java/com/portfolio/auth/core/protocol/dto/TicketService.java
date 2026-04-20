package com.portfolio.auth.core.protocol.dto;

import java.time.Instant;

/**
 * Ticket emitido por el TGS para ser presentado ante un servicio protegido.
 */
public record TicketService(
        String version,
        String ticketId,
        Instant issuedAt,
        Instant expiresAt,
        String clientId,
        String clientAddress,
        String serviceId,
        String clientServiceSessionKey
) {
}
