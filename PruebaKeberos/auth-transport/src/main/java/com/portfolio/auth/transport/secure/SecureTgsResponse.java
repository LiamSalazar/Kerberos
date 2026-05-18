package com.portfolio.auth.transport.secure;

import com.portfolio.auth.core.protocol.ProtocolMessage;
import com.portfolio.auth.crypto.CryptoEnvelope;

import java.time.Instant;

public record SecureTgsResponse(
        String version,
        String requestId,
        Instant issuedAt,
        Instant expiresAt,
        String clientId,
        String serviceId,
        String clientServiceSessionKey,
        CryptoEnvelope ticketServiceEnvelope
) implements ProtocolMessage {
}
