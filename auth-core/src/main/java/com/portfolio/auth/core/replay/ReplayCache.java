package com.portfolio.auth.core.replay;

import java.time.Instant;

/**
 * Contrato minimo para detectar reuso de requestId, authenticatorId o claves
 * compuestas durante una ventana temporal.
 */
public interface ReplayCache {
    /**
     * Registra una clave si no esta activa en la cache.
     *
     * @return true cuando la clave queda registrada; false cuando ya existia y
     *         aun no habia expirado.
     */
    boolean registerIfAbsent(String replayKey, Instant expiresAt);

    boolean contains(String replayKey);

    void purgeExpired();
}
