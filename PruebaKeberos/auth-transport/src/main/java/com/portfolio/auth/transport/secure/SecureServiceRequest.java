package com.portfolio.auth.transport.secure;

import com.portfolio.auth.core.protocol.ProtocolMessage;
import com.portfolio.auth.crypto.CryptoEnvelope;

import java.time.Instant;

public record SecureServiceRequest(
        String version,
        String requestId,
        Instant issuedAt,
        String clientId,
        String serviceId,
        CryptoEnvelope ticketServiceEnvelope,
        CryptoEnvelope clientAuthenticatorEnvelope
) implements ProtocolMessage {
}
