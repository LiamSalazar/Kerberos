package com.portfolio.auth.transport.secure;

import com.portfolio.auth.core.protocol.ProtocolMessage;
import com.portfolio.auth.crypto.CryptoEnvelope;

import java.time.Instant;

public record SecureTgsRequest(
        String version,
        String requestId,
        Instant issuedAt,
        String clientId,
        String serviceId,
        String ticketGrantingServerId,
        CryptoEnvelope ticketTgsEnvelope,
        CryptoEnvelope clientAuthenticatorEnvelope
) implements ProtocolMessage {
}
