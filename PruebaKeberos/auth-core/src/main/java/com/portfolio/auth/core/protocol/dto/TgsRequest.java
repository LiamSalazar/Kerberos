package com.portfolio.auth.core.protocol.dto;

import com.portfolio.auth.core.protocol.ProtocolMessage;

import java.time.Instant;

/**
 * Solicitud enviada al Ticket Granting Server para obtener
 * un ticket de acceso hacia un servicio concreto.
 */
public record TgsRequest(
        String version,
        String requestId,
        Instant issuedAt,
        String clientId,
        String serviceId,
        String ticketGrantingServerId,
        TicketTgs ticketTgs,
        ClientAuthenticator clientAuthenticator
) implements ProtocolMessage {
}
