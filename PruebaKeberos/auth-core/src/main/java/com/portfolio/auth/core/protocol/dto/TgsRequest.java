package com.portfolio.auth.core.protocol.dto;

import java.time.Instant;

/**
 * Solicitud enviada al Ticket Granting Server para obtener
 * un ticket de acceso hacia un servicio concreto.
 */
public record TgsRequest(
        String version,
        String requestId,
        String serviceId,
        Instant issuedAt,
        TicketTgs ticketTgs,
        ClientAuthenticator clientAuthenticator
) {
}
