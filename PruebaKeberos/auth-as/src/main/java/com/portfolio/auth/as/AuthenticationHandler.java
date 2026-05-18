package com.portfolio.auth.as;

import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.AsRequest;
import com.portfolio.auth.core.protocol.dto.ErrorResponse;
import com.portfolio.auth.core.protocol.dto.TicketTgs;
import com.portfolio.auth.crypto.CryptoEnvelope;
import com.portfolio.auth.crypto.SessionKeys;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.protocol.MessageHandler;
import com.portfolio.auth.transport.protocol.MessageType;
import com.portfolio.auth.transport.protocol.ProtocolEnvelope;
import com.portfolio.auth.transport.secure.SecureAad;
import com.portfolio.auth.transport.secure.SecureAsResponse;
import com.portfolio.auth.transport.secure.SecureJsonCrypto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AuthenticationHandler implements MessageHandler {
    private final AuthConfig config;
    private final InMemoryPrincipalRepository principals;
    private final JsonMessageCodec codec;
    private final SecureJsonCrypto secureJsonCrypto;

    public AuthenticationHandler(
            AuthConfig config,
            InMemoryPrincipalRepository principals,
            JsonMessageCodec codec,
            SecureJsonCrypto secureJsonCrypto) {
        this.config = Objects.requireNonNull(config, "config");
        this.principals = Objects.requireNonNull(principals, "principals");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.secureJsonCrypto = Objects.requireNonNull(secureJsonCrypto, "secureJsonCrypto");
    }

    @Override
    public ProtocolEnvelope handle(ProtocolEnvelope envelope) throws Exception {
        if (envelope.messageType() != MessageType.AS_REQUEST) {
            return error(envelope.requestId(), "AS_BAD_MESSAGE", "AS solo acepta AS_REQUEST");
        }

        AsRequest request = codec.decodePayload(envelope.payloadJson(), AsRequest.class);
        String clientSecret = principals.clientSecret(request.clientId()).orElse(null);
        String tgsSecret = principals.ticketGrantingServerSecret(request.ticketGrantingServerId()).orElse(null);
        if (clientSecret == null || tgsSecret == null) {
            return error(request.requestId(), "AS_UNKNOWN_PRINCIPAL", "Cliente o TGS no registrado");
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(config.ticketLifetime());
        String sessionKey = SessionKeys.randomSessionKey();
        TicketTgs ticket = new TicketTgs(
                request.version(),
                "tgt-" + UUID.randomUUID(),
                issuedAt,
                expiresAt,
                request.clientId(),
                config.clientHost(),
                request.ticketGrantingServerId(),
                sessionKey);
        CryptoEnvelope encryptedTicket = secureJsonCrypto.encrypt(ticket, tgsSecret, SecureAad.ticketTgs());
        SecureAsResponse response = new SecureAsResponse(
                request.version(),
                request.requestId(),
                issuedAt,
                expiresAt,
                request.clientId(),
                request.ticketGrantingServerId(),
                sessionKey,
                encryptedTicket);
        CryptoEnvelope encryptedResponse = secureJsonCrypto.encrypt(
                response,
                clientSecret,
                SecureAad.asResponse(request.requestId()));

        return new ProtocolEnvelope(
                MessageType.AS_RESPONSE,
                ProtocolDefaults.CURRENT_VERSION,
                request.requestId(),
                Instant.now(),
                codec.encodePayload(encryptedResponse));
    }

    private ProtocolEnvelope error(String requestId, String code, String message) {
        ErrorResponse error = new ErrorResponse(
                ProtocolDefaults.CURRENT_VERSION,
                requestId,
                Instant.now(),
                "auth-as",
                code,
                message,
                requestId);
        return new ProtocolEnvelope(
                MessageType.ERROR_RESPONSE,
                ProtocolDefaults.CURRENT_VERSION,
                requestId,
                Instant.now(),
                codec.encodePayload(error));
    }
}
