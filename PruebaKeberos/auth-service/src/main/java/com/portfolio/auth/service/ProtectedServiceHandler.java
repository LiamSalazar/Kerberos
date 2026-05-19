package com.portfolio.auth.service;

import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.ClientAuthenticator;
import com.portfolio.auth.core.protocol.dto.ErrorResponse;
import com.portfolio.auth.core.protocol.dto.ServiceResponse;
import com.portfolio.auth.core.protocol.dto.TicketService;
import com.portfolio.auth.core.replay.ReplayCache;
import com.portfolio.auth.crypto.CryptoEnvelope;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.protocol.MessageHandler;
import com.portfolio.auth.transport.protocol.MessageType;
import com.portfolio.auth.transport.protocol.ProtocolEnvelope;
import com.portfolio.auth.transport.secure.SecureAad;
import com.portfolio.auth.transport.secure.SecureJsonCrypto;
import com.portfolio.auth.transport.secure.SecureServiceRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class ProtectedServiceHandler implements MessageHandler {
    private final AuthConfig config;
    private final Map<String, String> serviceSecrets;
    private final ProtectedResource resource;
    private final ReplayCache replayCache;
    private final JsonMessageCodec codec;
    private final SecureJsonCrypto secureJsonCrypto;

    public ProtectedServiceHandler(
            AuthConfig config,
            Map<String, String> serviceSecrets,
            ProtectedResource resource,
            ReplayCache replayCache,
            JsonMessageCodec codec,
            SecureJsonCrypto secureJsonCrypto) {
        this.config = Objects.requireNonNull(config, "config");
        this.serviceSecrets = Map.copyOf(serviceSecrets);
        this.resource = Objects.requireNonNull(resource, "resource");
        this.replayCache = Objects.requireNonNull(replayCache, "replayCache");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.secureJsonCrypto = Objects.requireNonNull(secureJsonCrypto, "secureJsonCrypto");
    }

    public static Map<String, String> defaultSecrets(AuthConfig config) {
        return Map.of(config.defaultServiceId(), config.demoServiceSecret());
    }

    @Override
    public ProtocolEnvelope handle(ProtocolEnvelope envelope) throws Exception {
        if (envelope.messageType() != MessageType.SERVICE_REQUEST) {
            return error(envelope.requestId(), "SERVICE_BAD_MESSAGE", "Service solo acepta SERVICE_REQUEST");
        }

        SecureServiceRequest request = codec.decodePayload(envelope.payloadJson(), SecureServiceRequest.class);
        if (!replayCache.registerIfAbsent("service-request:" + request.requestId(),
                request.issuedAt().plus(config.replayWindow()))) {
            return error(request.requestId(), "SERVICE_REPLAY", "requestId de servicio repetido");
        }

        String serviceSecret = serviceSecrets.get(request.serviceId());
        if (serviceSecret == null) {
            return error(request.requestId(), "SERVICE_UNKNOWN", "Servicio no registrado");
        }

        TicketService ticket = secureJsonCrypto.decrypt(
                request.ticketServiceEnvelope(),
                serviceSecret,
                SecureAad.ticketService(),
                TicketService.class);
        ClientAuthenticator authenticator = secureJsonCrypto.decrypt(
                request.clientAuthenticatorEnvelope(),
                ticket.clientServiceSessionKey(),
                SecureAad.authenticator(request.requestId()),
                ClientAuthenticator.class);

        String validationError = validate(request, ticket, authenticator);
        if (validationError != null) {
            return error(request.requestId(), "SERVICE_INVALID_AUTHENTICATOR", validationError);
        }

        if (!replayCache.registerIfAbsent("service-auth:" + ticket.ticketId() + ":" + authenticator.requestId(),
                authenticator.expiresAt())) {
            return error(request.requestId(), "SERVICE_REPLAY", "Autenticador de servicio repetido");
        }

        Instant now = Instant.now();
        ServiceResponse response = new ServiceResponse(
                request.version(),
                request.requestId(),
                now,
                ticket.expiresAt(),
                ticket.clientId(),
                ticket.serviceId(),
                authenticator.issuedAt(),
                now,
                resource.read(),
                true);
        CryptoEnvelope encryptedResponse = secureJsonCrypto.encrypt(
                response,
                ticket.clientServiceSessionKey(),
                SecureAad.serviceResponse(request.requestId()));

        return new ProtocolEnvelope(
                MessageType.SERVICE_RESPONSE,
                ProtocolDefaults.CURRENT_VERSION,
                request.requestId(),
                Instant.now(),
                codec.encodePayload(encryptedResponse));
    }

    private String validate(SecureServiceRequest request, TicketService ticket, ClientAuthenticator authenticator) {
        Instant now = Instant.now();
        if (!ticket.serviceId().equals(request.serviceId())) {
            return "Ticket de servicio no corresponde al servicio solicitado";
        }
        if (!ticket.clientId().equals(request.clientId()) || !authenticator.clientId().equals(request.clientId())) {
            return "Identidad de cliente inconsistente";
        }
        if (!ticket.clientAddress().equals(authenticator.clientAddress())) {
            return "Direccion de cliente inconsistente";
        }
        if (!ticket.expiresAt().isAfter(now)) {
            return "Ticket de servicio expirado";
        }
        if (!authenticator.expiresAt().isAfter(now)) {
            return "Autenticador de servicio expirado";
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
                "auth-service",
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
