package com.portfolio.auth.crypto;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Genera secretos efimeros para sesiones modulares cliente-TGS y
 * cliente-servicio.
 */
public final class SessionKeys {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SESSION_KEY_BYTES = 32;

    private SessionKeys() {
    }

    public static String randomSessionKey() {
        byte[] bytes = new byte[SESSION_KEY_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
