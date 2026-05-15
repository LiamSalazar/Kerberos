package com.portfolio.auth.core.protocol.dto;

import com.portfolio.auth.core.protocol.ProtocolMessage;

import java.time.Instant;

/**
 * Respuesta del Authentication Server con la sesion cliente-TGS
 * y el Ticket TGS emitido.
 */
public record AsResponse(
        String version,
        String requestId,
        Instant issuedAt,
        Instant expiresAt,
        String clientId,
        String ticketGrantingServerId,
        String clientTgsSessionKey,
        TicketTgs ticketTgs
) implements ProtocolMessage {
}
