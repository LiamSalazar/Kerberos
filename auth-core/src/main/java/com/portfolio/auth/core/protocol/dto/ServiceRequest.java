package com.portfolio.auth.core.protocol.dto;

import com.portfolio.auth.core.protocol.ProtocolMessage;

import java.time.Instant;

/**
 * Solicitud enviada al servicio protegido con el ticket de servicio
 * y el autenticador del cliente.
 */
public record ServiceRequest(
        String version,
        String requestId,
        Instant issuedAt,
        String clientId,
        String serviceId,
        TicketService ticketService,
        ClientAuthenticator clientAuthenticator
) implements ProtocolMessage {
}
