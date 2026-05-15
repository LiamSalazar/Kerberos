package com.portfolio.auth.crypto;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Implementacion AEAD con AES-GCM. No reemplaza automaticamente a AESUtils
 * legacy; deja lista la ruta de migracion con IV transportado en envelope.
 */
public final class AesGcmCryptoService implements AeadCryptoService {
    public static final String ENVELOPE_VERSION = "crypto-envelope/1";
    public static final String ALGORITHM = "AES/GCM/NoPadding";
    public static final int IV_BYTES = 12;
    public static final int TAG_BITS = 128;

    private final SecureRandom secureRandom;
    private final Clock clock;

    public AesGcmCryptoService() {
        this(new SecureRandom(), Clock.systemUTC());
    }

    AesGcmCryptoService(SecureRandom secureRandom, Clock clock) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public CryptoEnvelope encrypt(byte[] plaintext, SecretKey key, byte[] associatedData)
            throws GeneralSecurityException {
        Objects.requireNonNull(plaintext, "plaintext");
        Objects.requireNonNull(key, "key");

        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        if (associatedData != null && associatedData.length > 0) {
            cipher.updateAAD(associatedData);
        }

        return new CryptoEnvelope(
                ENVELOPE_VERSION,
                ALGORITHM,
                iv,
                cipher.doFinal(plaintext),
                Instant.now(clock));
    }

    @Override
    public byte[] decrypt(CryptoEnvelope envelope, SecretKey key, byte[] associatedData)
            throws GeneralSecurityException {
        Objects.requireNonNull(envelope, "envelope");
        Objects.requireNonNull(key, "key");

        if (!ALGORITHM.equals(envelope.algorithm())) {
            throw new GeneralSecurityException("Algoritmo no soportado: " + envelope.algorithm());
        }

        Cipher cipher = Cipher.getInstance(envelope.algorithm());
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, envelope.iv()));
        if (associatedData != null && associatedData.length > 0) {
            cipher.updateAAD(associatedData);
        }

        return cipher.doFinal(envelope.ciphertext());
    }
}
