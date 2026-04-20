package com.portfolio.auth.core.protocol.dto;

import java.time.Instant;

/**
 * Ticket emitido por el AS para ser presentado ante el TGS.
 */
public record TicketTgs(
        String version,
        String ticketId,
        Instant issuedAt,
        Instant expiresAt,
        String clientId,
        String clientAddress,
        String ticketGrantingServerId,
        String clientTgsSessionKey
) {
}
