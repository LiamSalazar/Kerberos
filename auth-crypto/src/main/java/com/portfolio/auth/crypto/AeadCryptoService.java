package com.portfolio.auth.crypto;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

public interface AeadCryptoService {
    CryptoEnvelope encrypt(byte[] plaintext, SecretKey key, byte[] associatedData) throws GeneralSecurityException;

    byte[] decrypt(CryptoEnvelope envelope, SecretKey key, byte[] associatedData) throws GeneralSecurityException;
}
