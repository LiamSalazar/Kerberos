package com.portfolio.auth.core.protocol.dto;

import com.portfolio.auth.core.protocol.ProtocolMessage;

import java.time.Instant;

/**
 * Respuesta del servicio al cliente una vez validado el acceso.
 */
public record ServiceResponse(
        String version,
        String requestId,
        Instant issuedAt,
        Instant expiresAt,
        String clientId,
        String serviceId,
        Instant clientAuthenticatorIssuedAt,
        Instant serverValidatedAt,
        String serviceMessage,
        boolean accessGranted
) implements ProtocolMessage {
}
