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
}
