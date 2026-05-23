package com.portfolio.auth.core.session;

import java.security.SecureRandom;
import java.util.Base64;

public final class SecureSessionIdGenerator {
    private static final int DEFAULT_BYTES = 32;

    private final SecureRandom random;
    private final int byteCount;

    public SecureSessionIdGenerator() {
        this(new SecureRandom(), DEFAULT_BYTES);
    }

    SecureSessionIdGenerator(SecureRandom random, int byteCount) {
        if (byteCount < 24) {
            throw new IllegalArgumentException("byteCount must be at least 24");
        }
        this.random = random;
        this.byteCount = byteCount;
    }

    public String newSessionId() {
        byte[] bytes = new byte[byteCount];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
