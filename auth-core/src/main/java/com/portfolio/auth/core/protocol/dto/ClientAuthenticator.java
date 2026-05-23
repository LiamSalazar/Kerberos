package com.portfolio.auth.core.protocol.dto;

import com.portfolio.auth.core.protocol.ProtocolMessage;

import java.time.Instant;

/**
 * Autenticador efimero presentado por el cliente junto con un ticket.
 */
public record ClientAuthenticator(
        String version,
        String requestId,
        Instant issuedAt,
        Instant expiresAt,
        String clientId,
        String clientAddress
) implements ProtocolMessage {
}
