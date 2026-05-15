package com.portfolio.auth.crypto;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/**
 * Sobre criptografico para transportar nonces y ciphertext de cifrado
 * autenticado. Esta pensado para la migracion gradual fuera de AES/CBC legacy.
 */
public record CryptoEnvelope(
        String version,
        String algorithm,
        byte[] iv,
        byte[] ciphertext,
        Instant issuedAt
) implements Serializable {
    public CryptoEnvelope {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(iv, "iv");
        Objects.requireNonNull(ciphertext, "ciphertext");
        Objects.requireNonNull(issuedAt, "issuedAt");
        iv = Arrays.copyOf(iv, iv.length);
        ciphertext = Arrays.copyOf(ciphertext, ciphertext.length);
    }

    @Override
    public byte[] iv() {
        return Arrays.copyOf(iv, iv.length);
    }

    @Override
    public byte[] ciphertext() {
        return Arrays.copyOf(ciphertext, ciphertext.length);
    }
}
