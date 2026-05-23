package com.portfolio.auth.core.protocol;

import java.io.Serializable;
import java.time.Instant;

/**
 * Contrato minimo para mensajes de protocolo transportables.
 */
public interface ProtocolMessage extends Serializable {
    String version();

    String requestId();

    Instant issuedAt();
}
