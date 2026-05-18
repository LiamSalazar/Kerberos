package com.portfolio.auth.transport.secure;

import java.nio.charset.StandardCharsets;

/**
 * Associated data estable para AES-GCM en la ruta modular.
 */
public final class SecureAad {
    private SecureAad() {
    }

    public static byte[] ticketTgs() {
        return bytes("auth/1:ticket-tgs");
    }

    public static byte[] ticketService() {
        return bytes("auth/1:ticket-service");
    }

    public static byte[] authenticator(String requestId) {
        return bytes("auth/1:authenticator:" + requestId);
    }

    public static byte[] asResponse(String requestId) {
        return bytes("auth/1:as-response:" + requestId);
    }

    public static byte[] tgsResponse(String requestId) {
        return bytes("auth/1:tgs-response:" + requestId);
    }

    public static byte[] serviceResponse(String requestId) {
        return bytes("auth/1:service-response:" + requestId);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
