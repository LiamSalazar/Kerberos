package com.portfolio.auth.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesGcmCryptoServiceTest {

    @Test
    void shouldEncryptAndDecryptWithAssociatedData() throws Exception {
        AesGcmCryptoService service = new AesGcmCryptoService();
        SecretKey key = generateAesKey();
        byte[] plaintext = "ticket-service-payload".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "auth/1:TGS-REP".getBytes(StandardCharsets.UTF_8);

        CryptoEnvelope envelope = service.encrypt(plaintext, key, aad);
        byte[] decrypted = service.decrypt(envelope, key, aad);

        assertArrayEquals(plaintext, decrypted);
        assertFalse(Arrays.equals(plaintext, envelope.ciphertext()));
    }

    @Test
    void shouldRejectTamperedCiphertext() throws Exception {
        AesGcmCryptoService service = new AesGcmCryptoService();
        SecretKey key = generateAesKey();
        byte[] aad = "auth/1:AS-REP".getBytes(StandardCharsets.UTF_8);

        CryptoEnvelope envelope = service.encrypt("payload".getBytes(StandardCharsets.UTF_8), key, aad);
        byte[] tamperedCiphertext = envelope.ciphertext();
        tamperedCiphertext[0] = (byte) (tamperedCiphertext[0] ^ 1);

        CryptoEnvelope tampered = new CryptoEnvelope(
                envelope.version(),
                envelope.algorithm(),
                envelope.iv(),
                tamperedCiphertext,
                envelope.issuedAt());

        assertThrows(GeneralSecurityException.class, () -> service.decrypt(tampered, key, aad));
    }

    @Test
    void shouldRejectDifferentAssociatedData() throws Exception {
        AesGcmCryptoService service = new AesGcmCryptoService();
        SecretKey key = generateAesKey();

        CryptoEnvelope envelope = service.encrypt(
                "payload".getBytes(StandardCharsets.UTF_8),
                key,
                "expected-aad".getBytes(StandardCharsets.UTF_8));

        assertThrows(GeneralSecurityException.class,
                () -> service.decrypt(envelope, key, "wrong-aad".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void shouldDefensivelyCopyEnvelopeByteArrays() {
        byte[] iv = new byte[] { 1, 2, 3 };
        byte[] ciphertext = new byte[] { 4, 5, 6 };
        CryptoEnvelope envelope = new CryptoEnvelope(
                AesGcmCryptoService.ENVELOPE_VERSION,
                AesGcmCryptoService.ALGORITHM,
                iv,
                ciphertext,
                Instant.parse("2026-05-12T10:00:00Z"));

        iv[0] = 99;
        ciphertext[0] = 99;
        byte[] envelopeIv = envelope.iv();
        byte[] envelopeCiphertext = envelope.ciphertext();
        envelopeIv[1] = 88;
        envelopeCiphertext[1] = 88;

        assertArrayEquals(new byte[] { 1, 2, 3 }, envelope.iv());
        assertArrayEquals(new byte[] { 4, 5, 6 }, envelope.ciphertext());
    }

    private static SecretKey generateAesKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        return keyGenerator.generateKey();
    }
}
