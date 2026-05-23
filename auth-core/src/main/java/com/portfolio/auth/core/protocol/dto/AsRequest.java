package com.portfolio.auth.core.protocol.dto;

import com.portfolio.auth.core.protocol.ProtocolMessage;

import java.time.Instant;

/**
 * Solicitud enviada por el cliente al Authentication Server para iniciar
 * el flujo y pedir un Ticket Granting Ticket.
 */
public record AsRequest(
        String version,
        String requestId,
        String clientId,
        String ticketGrantingServerId,
        Instant issuedAt
) implements ProtocolMessage {
}
