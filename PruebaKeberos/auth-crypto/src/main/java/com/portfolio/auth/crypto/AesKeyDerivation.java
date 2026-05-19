package com.portfolio.auth.crypto;

import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;
import java.util.Objects;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Derivacion de claves para la ruta modular.
 */
public final class AesKeyDerivation {
    public static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final int ITERATIONS = 65_536;
    public static final int KEY_BITS = 256;

    private AesKeyDerivation() {
    }

    public static SecretKey fromPassphrase(String passphrase, String salt) throws GeneralSecurityException {
        Objects.requireNonNull(passphrase, "passphrase");
        Objects.requireNonNull(salt, "salt");

        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                ITERATIONS, KEY_BITS);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }
}
