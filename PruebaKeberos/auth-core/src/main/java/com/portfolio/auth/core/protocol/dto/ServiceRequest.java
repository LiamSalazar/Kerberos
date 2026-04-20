package com.portfolio.auth.core.protocol.dto;

import java.time.Instant;

/**
 * Solicitud enviada al servicio protegido con el ticket de servicio
 * y el autenticador del cliente.
 */
public record ServiceRequest(
        String version,
        String requestId,
        Instant issuedAt,
        String serviceId,
        TicketService ticketService,
        ClientAuthenticator clientAuthenticator
) {
}
