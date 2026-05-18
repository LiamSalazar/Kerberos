package com.portfolio.auth.transport.protocol;

import java.time.Instant;
import java.util.Objects;

/**
 * Envoltorio JSON de la ruta modular. El payload se mantiene como JSON crudo
 * para permitir cifrar respuestas completas con CryptoEnvelope.
 */
public record ProtocolEnvelope(
        MessageType messageType,
        String version,
        String requestId,
        Instant issuedAt,
        String payloadJson
) {
    public ProtocolEnvelope {
        Objects.requireNonNull(messageType, "messageType");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(payloadJson, "payloadJson");
    }
}
