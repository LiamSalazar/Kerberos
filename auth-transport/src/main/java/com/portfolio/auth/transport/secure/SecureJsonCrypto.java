package com.portfolio.auth.transport.secure;

import com.portfolio.auth.crypto.AesGcmCryptoService;
import com.portfolio.auth.crypto.AesKeyDerivation;
import com.portfolio.auth.crypto.CryptoEnvelope;
import com.portfolio.auth.transport.json.JsonMessageCodec;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Objects;

import javax.crypto.SecretKey;

public final class SecureJsonCrypto {
    private final JsonMessageCodec codec;
    private final AesGcmCryptoService cryptoService;
    private final String salt;

    public SecureJsonCrypto(JsonMessageCodec codec, AesGcmCryptoService cryptoService, String salt) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.cryptoService = Objects.requireNonNull(cryptoService, "cryptoService");
        this.salt = Objects.requireNonNull(salt, "salt");
    }

    public CryptoEnvelope encrypt(Object payload, String secret, byte[] associatedData) throws GeneralSecurityException {
        String json = codec.encodePayload(payload);
        return cryptoService.encrypt(json.getBytes(StandardCharsets.UTF_8), key(secret), associatedData);
    }

    public <T> T decrypt(CryptoEnvelope envelope, String secret, byte[] associatedData, Class<T> type)
            throws GeneralSecurityException {
        byte[] plaintext = cryptoService.decrypt(envelope, key(secret), associatedData);
        return codec.decodePayload(new String(plaintext, StandardCharsets.UTF_8), type);
    }

    private SecretKey key(String secret) throws GeneralSecurityException {
        return AesKeyDerivation.fromPassphrase(secret, salt);
    }
}
