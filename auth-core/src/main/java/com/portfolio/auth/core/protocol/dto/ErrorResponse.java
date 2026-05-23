package com.portfolio.auth.core.protocol.dto;

import com.portfolio.auth.core.protocol.ProtocolMessage;

import java.time.Instant;

/**
 * Error tipado del protocolo, util para AS, TGS o Service.
 */
public record ErrorResponse(
        String version,
        String requestId,
        Instant issuedAt,
        String source,
        String errorCode,
        String errorMessage,
        String failedRequestId
) implements ProtocolMessage {
}
