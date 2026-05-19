package com.portfolio.auth.transport.secure;

import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.ClientAuthenticator;
import com.portfolio.auth.core.protocol.dto.TicketTgs;
import com.portfolio.auth.crypto.AesGcmCryptoService;
import com.portfolio.auth.crypto.CryptoEnvelope;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecureJsonCryptoTest {

    @Test
    void shouldEncryptAndDecryptTicketTgsWithAesGcmEnvelope() throws Exception {
        JsonMessageCodec codec = new JsonMessageCodec();
        SecureJsonCrypto crypto = new SecureJsonCrypto(codec, new AesGcmCryptoService(), "test-salt");
        TicketTgs ticket = new TicketTgs(
                ProtocolDefaults.CURRENT_VERSION,
                "ticket-1",
                Instant.parse("2026-05-18T10:00:00Z"),
                Instant.parse("2026-05-18T10:05:00Z"),
                "client-1",
                "127.0.0.1",
                "tgs-1",
                "session-key");

        CryptoEnvelope envelope = crypto.encrypt(ticket, "tgs-secret", SecureAad.ticketTgs());
        TicketTgs decrypted = crypto.decrypt(envelope, "tgs-secret", SecureAad.ticketTgs(), TicketTgs.class);

        assertEquals(ticket, decrypted);
    }

    @Test
    void shouldRejectAuthenticatorWithWrongAssociatedData() throws Exception {
        JsonMessageCodec codec = new JsonMessageCodec();
        SecureJsonCrypto crypto = new SecureJsonCrypto(codec, new AesGcmCryptoService(), "test-salt");
        ClientAuthenticator authenticator = new ClientAuthenticator(
                ProtocolDefaults.CURRENT_VERSION,
                "auth-1",
                Instant.parse("2026-05-18T10:00:00Z"),
                Instant.parse("2026-05-18T10:05:00Z"),
                "client-1",
                "127.0.0.1");

        CryptoEnvelope envelope = crypto.encrypt(authenticator, "session-key", SecureAad.authenticator("req-1"));

        assertThrows(GeneralSecurityException.class,
                () -> crypto.decrypt(envelope, "session-key", SecureAad.authenticator("req-2"),
                        ClientAuthenticator.class));
    }

    @Test
    void shouldRejectCorruptedCiphertext() throws Exception {
        JsonMessageCodec codec = new JsonMessageCodec();
        SecureJsonCrypto crypto = new SecureJsonCrypto(codec, new AesGcmCryptoService(), "test-salt");
        ClientAuthenticator authenticator = authenticator();
        CryptoEnvelope envelope = crypto.encrypt(authenticator, "session-key", SecureAad.authenticator("req-1"));
        byte[] corruptedCiphertext = envelope.ciphertext();
        corruptedCiphertext[0] = (byte) (corruptedCiphertext[0] ^ 0x01);
        CryptoEnvelope corrupted = new CryptoEnvelope(
                envelope.version(),
                envelope.algorithm(),
                envelope.iv(),
                corruptedCiphertext,
                envelope.issuedAt());

        assertThrows(GeneralSecurityException.class,
                () -> crypto.decrypt(corrupted, "session-key", SecureAad.authenticator("req-1"),
                        ClientAuthenticator.class));
    }

    @Test
    void shouldRejectAlteredCryptoEnvelope() throws Exception {
        JsonMessageCodec codec = new JsonMessageCodec();
        SecureJsonCrypto crypto = new SecureJsonCrypto(codec, new AesGcmCryptoService(), "test-salt");
        ClientAuthenticator authenticator = authenticator();
        CryptoEnvelope envelope = crypto.encrypt(authenticator, "session-key", SecureAad.authenticator("req-1"));
        byte[] alteredIv = Arrays.copyOf(envelope.iv(), envelope.iv().length);
        alteredIv[0] = (byte) (alteredIv[0] ^ 0x01);
        CryptoEnvelope altered = new CryptoEnvelope(
                envelope.version(),
                envelope.algorithm(),
                alteredIv,
                envelope.ciphertext(),
                envelope.issuedAt());

        assertThrows(GeneralSecurityException.class,
                () -> crypto.decrypt(altered, "session-key", SecureAad.authenticator("req-1"),
                        ClientAuthenticator.class));
    }

    @Test
    void shouldRejectWrongSecret() throws Exception {
        JsonMessageCodec codec = new JsonMessageCodec();
        SecureJsonCrypto crypto = new SecureJsonCrypto(codec, new AesGcmCryptoService(), "test-salt");
        ClientAuthenticator authenticator = authenticator();
        CryptoEnvelope envelope = crypto.encrypt(authenticator, "session-key", SecureAad.authenticator("req-1"));

        assertThrows(GeneralSecurityException.class,
                () -> crypto.decrypt(envelope, "wrong-session-key", SecureAad.authenticator("req-1"),
                        ClientAuthenticator.class));
    }

    private static ClientAuthenticator authenticator() {
        return new ClientAuthenticator(
                ProtocolDefaults.CURRENT_VERSION,
                "auth-1",
                Instant.parse("2026-05-18T10:00:00Z"),
                Instant.parse("2026-05-18T10:05:00Z"),
                "client-1",
                "127.0.0.1");
    }
}
