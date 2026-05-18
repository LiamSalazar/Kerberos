package com.portfolio.auth.tgs;

import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.ClientAuthenticator;
import com.portfolio.auth.core.protocol.dto.ErrorResponse;
import com.portfolio.auth.core.protocol.dto.TicketService;
import com.portfolio.auth.core.protocol.dto.TicketTgs;
import com.portfolio.auth.core.replay.ReplayCache;
import com.portfolio.auth.crypto.CryptoEnvelope;
import com.portfolio.auth.crypto.SessionKeys;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.protocol.MessageHandler;
import com.portfolio.auth.transport.protocol.MessageType;
import com.portfolio.auth.transport.protocol.ProtocolEnvelope;
import com.portfolio.auth.transport.secure.SecureAad;
import com.portfolio.auth.transport.secure.SecureJsonCrypto;
import com.portfolio.auth.transport.secure.SecureTgsRequest;
import com.portfolio.auth.transport.secure.SecureTgsResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class TicketGrantingHandler implements MessageHandler {
    private final AuthConfig config;
    private final InMemoryServiceRegistry registry;
    private final ReplayCache replayCache;
    private final JsonMessageCodec codec;
    private final SecureJsonCrypto secureJsonCrypto;

    public TicketGrantingHandler(
            AuthConfig config,
            InMemoryServiceRegistry registry,
            ReplayCache replayCache,
            JsonMessageCodec codec,
            SecureJsonCrypto secureJsonCrypto) {
        this.config = Objects.requireNonNull(config, "config");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.replayCache = Objects.requireNonNull(replayCache, "replayCache");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.secureJsonCrypto = Objects.requireNonNull(secureJsonCrypto, "secureJsonCrypto");
    }

    @Override
    public ProtocolEnvelope handle(ProtocolEnvelope envelope) throws Exception {
        if (envelope.messageType() != MessageType.TGS_REQUEST) {
            return error(envelope.requestId(), "TGS_BAD_MESSAGE", "TGS solo acepta TGS_REQUEST");
        }

        SecureTgsRequest request = codec.decodePayload(envelope.payloadJson(), SecureTgsRequest.class);
        if (!replayCache.registerIfAbsent("tgs-request:" + request.requestId(),
                request.issuedAt().plus(config.replayWindow()))) {
            return error(request.requestId(), "TGS_REPLAY", "requestId TGS repetido");
        }

        String tgsSecret = registry.ticketGrantingServerSecret(request.ticketGrantingServerId()).orElse(null);
        String serviceSecret = registry.serviceSecret(request.serviceId()).orElse(null);
        if (tgsSecret == null || serviceSecret == null) {
            return error(request.requestId(), "TGS_UNKNOWN_SERVICE", "TGS o servicio no registrado");
        }

        TicketTgs ticket = secureJsonCrypto.decrypt(
                request.ticketTgsEnvelope(),
                tgsSecret,
                SecureAad.ticketTgs(),
                TicketTgs.class);
        ClientAuthenticator authenticator = secureJsonCrypto.decrypt(
                request.clientAuthenticatorEnvelope(),
                ticket.clientTgsSessionKey(),
                SecureAad.authenticator(request.requestId()),
                ClientAuthenticator.class);

        String validationError = validate(request, ticket, authenticator);
        if (validationError != null) {
            return error(request.requestId(), "TGS_INVALID_AUTHENTICATOR", validationError);
        }

        if (!replayCache.registerIfAbsent("tgs-auth:" + ticket.ticketId() + ":" + authenticator.requestId(),
                authenticator.expiresAt())) {
            return error(request.requestId(), "TGS_REPLAY", "Autenticador TGS repetido");
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = min(ticket.expiresAt(), issuedAt.plus(config.ticketLifetime()));
        String serviceSessionKey = SessionKeys.randomSessionKey();
        TicketService serviceTicket = new TicketService(
                request.version(),
                "svc-ticket-" + UUID.randomUUID(),
                issuedAt,
                expiresAt,
                ticket.clientId(),
                ticket.clientAddress(),
                request.serviceId(),
                serviceSessionKey);
        CryptoEnvelope encryptedServiceTicket = secureJsonCrypto.encrypt(
                serviceTicket,
                serviceSecret,
                SecureAad.ticketService());
        SecureTgsResponse response = new SecureTgsResponse(
                request.version(),
                request.requestId(),
                issuedAt,
                expiresAt,
                ticket.clientId(),
                request.serviceId(),
                serviceSessionKey,
                encryptedServiceTicket);
        CryptoEnvelope encryptedResponse = secureJsonCrypto.encrypt(
                response,
                ticket.clientTgsSessionKey(),
                SecureAad.tgsResponse(request.requestId()));

        return new ProtocolEnvelope(
                MessageType.TGS_RESPONSE,
                ProtocolDefaults.CURRENT_VERSION,
                request.requestId(),
                Instant.now(),
                codec.encodePayload(encryptedResponse));
    }

    private String validate(SecureTgsRequest request, TicketTgs ticket, ClientAuthenticator authenticator) {
        Instant now = Instant.now();
        if (!ticket.ticketGrantingServerId().equals(request.ticketGrantingServerId())) {
            return "El ticket TGS no corresponde al TGS solicitado";
        }
        if (!ticket.clientId().equals(request.clientId()) || !authenticator.clientId().equals(request.clientId())) {
            return "Identidad de cliente inconsistente";
        }
        if (!ticket.clientAddress().equals(authenticator.clientAddress())) {
            return "Direccion de cliente inconsistente";
        }
        if (!ticket.expiresAt().isAfter(now)) {
            return "Ticket TGS expirado";
        }
        if (!authenticator.expiresAt().isAfter(now)) {
            return "Autenticador TGS expirado";
        }
        Duration skew = Duration.between(authenticator.issuedAt(), now).abs();
        if (skew.compareTo(config.allowedClockSkew()) > 0) {
            return "Autenticador fuera de clock skew permitido";
        }
        return null;
    }

    private ProtocolEnvelope error(String requestId, String code, String message) {
        ErrorResponse error = new ErrorResponse(
                ProtocolDefaults.CURRENT_VERSION,
                requestId,
                Instant.now(),
                "auth-tgs",
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

    private static Instant min(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }
}
