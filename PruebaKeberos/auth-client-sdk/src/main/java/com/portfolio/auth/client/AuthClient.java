package com.portfolio.auth.client;

import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.AsRequest;
import com.portfolio.auth.core.protocol.dto.ClientAuthenticator;
import com.portfolio.auth.core.protocol.dto.ErrorResponse;
import com.portfolio.auth.core.protocol.dto.ServiceResponse;
import com.portfolio.auth.crypto.AesGcmCryptoService;
import com.portfolio.auth.crypto.CryptoEnvelope;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.protocol.MessageType;
import com.portfolio.auth.transport.protocol.ProtocolEnvelope;
import com.portfolio.auth.transport.secure.SecureAad;
import com.portfolio.auth.transport.secure.SecureAsResponse;
import com.portfolio.auth.transport.secure.SecureJsonCrypto;
import com.portfolio.auth.transport.secure.SecureServiceRequest;
import com.portfolio.auth.transport.secure.SecureTgsRequest;
import com.portfolio.auth.transport.secure.SecureTgsResponse;
import com.portfolio.auth.transport.tcp.TcpMessageClient;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AuthClient {
    private final AuthConfig config;
    private final JsonMessageCodec codec;
    private final TcpMessageClient tcpClient;
    private final SecureJsonCrypto secureJsonCrypto;
    private final String asHost;
    private final int asPort;
    private final String tgsHost;
    private final int tgsPort;
    private final String serviceHost;
    private final int servicePort;

    public AuthClient(AuthConfig config) {
        this(
                config,
                config.authenticationServerHost(),
                config.authenticationServerPort(),
                config.ticketGrantingServerHost(),
                config.ticketGrantingServerPort(),
                config.serviceServerHost(),
                config.serviceServerPort());
    }

    public AuthClient(
            AuthConfig config,
            String host,
            int asPort,
            int tgsPort,
            int servicePort) {
        this(config, host, asPort, host, tgsPort, host, servicePort);
    }

    public AuthClient(
            AuthConfig config,
            String asHost,
            int asPort,
            String tgsHost,
            int tgsPort,
            String serviceHost,
            int servicePort) {
        this.config = Objects.requireNonNull(config, "config");
        this.codec = new JsonMessageCodec();
        this.tcpClient = new TcpMessageClient(codec);
        this.secureJsonCrypto = new SecureJsonCrypto(
                codec,
                new AesGcmCryptoService(),
                config.legacyPbkdf2Salt());
        this.asHost = Objects.requireNonNull(asHost, "asHost");
        this.asPort = asPort;
        this.tgsHost = Objects.requireNonNull(tgsHost, "tgsHost");
        this.tgsPort = tgsPort;
        this.serviceHost = Objects.requireNonNull(serviceHost, "serviceHost");
        this.servicePort = servicePort;
    }

    public ServiceResponse runFullFlow() throws Exception {
        SecureAsResponse asResponse = requestTicketGrantingTicket();
        SecureTgsResponse tgsResponse = requestServiceTicket(asResponse, config.defaultServiceId());
        return requestProtectedService(tgsResponse);
    }

    public SecureAsResponse requestTicketGrantingTicket() throws Exception {
        return requestTicketGrantingTicket("as-" + UUID.randomUUID());
    }

    public SecureAsResponse requestTicketGrantingTicket(String requestId) throws Exception {
        AsRequest request = new AsRequest(
                ProtocolDefaults.CURRENT_VERSION,
                requestId,
                config.defaultClientId(),
                config.defaultTicketGrantingServerId(),
                Instant.now());
        ProtocolEnvelope response = tcpClient.send(
                asHost,
                asPort,
                envelope(MessageType.AS_REQUEST, request.requestId(), request));
        CryptoEnvelope encryptedResponse = encryptedPayloadOrThrow(response, MessageType.AS_RESPONSE);
        return secureJsonCrypto.decrypt(
                encryptedResponse,
                config.legacyClientSecret(),
                SecureAad.asResponse(request.requestId()),
                SecureAsResponse.class);
    }

    public SecureTgsResponse requestServiceTicket(SecureAsResponse asResponse, String serviceId) throws Exception {
        return requestServiceTicket(
                asResponse,
                serviceId,
                "tgs-" + UUID.randomUUID(),
                "auth-tgs-" + UUID.randomUUID(),
                Instant.now());
    }

    public SecureTgsResponse requestServiceTicket(
            SecureAsResponse asResponse,
            String serviceId,
            String requestId,
            String authenticatorId,
            Instant authenticatorIssuedAt) throws Exception {
        ClientAuthenticator authenticator = authenticator(authenticatorId, authenticatorIssuedAt);
        CryptoEnvelope encryptedAuthenticator = secureJsonCrypto.encrypt(
                authenticator,
                asResponse.clientTgsSessionKey(),
                SecureAad.authenticator(requestId));
        SecureTgsRequest request = new SecureTgsRequest(
                ProtocolDefaults.CURRENT_VERSION,
                requestId,
                Instant.now(),
                config.defaultClientId(),
                serviceId,
                config.defaultTicketGrantingServerId(),
                asResponse.ticketTgsEnvelope(),
                encryptedAuthenticator);
        ProtocolEnvelope response = tcpClient.send(
                tgsHost,
                tgsPort,
                envelope(MessageType.TGS_REQUEST, request.requestId(), request));
        CryptoEnvelope encryptedResponse = encryptedPayloadOrThrow(response, MessageType.TGS_RESPONSE);
        return secureJsonCrypto.decrypt(
                encryptedResponse,
                asResponse.clientTgsSessionKey(),
                SecureAad.tgsResponse(request.requestId()),
                SecureTgsResponse.class);
    }

    public ServiceResponse requestProtectedService(SecureTgsResponse tgsResponse) throws Exception {
        return requestProtectedService(
                tgsResponse,
                "service-" + UUID.randomUUID(),
                "auth-service-" + UUID.randomUUID(),
                Instant.now());
    }

    public ServiceResponse requestProtectedService(
            SecureTgsResponse tgsResponse,
            String requestId,
            String authenticatorId,
            Instant authenticatorIssuedAt) throws Exception {
        ClientAuthenticator authenticator = authenticator(authenticatorId, authenticatorIssuedAt);
        CryptoEnvelope encryptedAuthenticator = secureJsonCrypto.encrypt(
                authenticator,
                tgsResponse.clientServiceSessionKey(),
                SecureAad.authenticator(requestId));
        SecureServiceRequest request = new SecureServiceRequest(
                ProtocolDefaults.CURRENT_VERSION,
                requestId,
                Instant.now(),
                config.defaultClientId(),
                tgsResponse.serviceId(),
                tgsResponse.ticketServiceEnvelope(),
                encryptedAuthenticator);
        ProtocolEnvelope response = tcpClient.send(
                serviceHost,
                servicePort,
                envelope(MessageType.SERVICE_REQUEST, request.requestId(), request));
        CryptoEnvelope encryptedResponse = encryptedPayloadOrThrow(response, MessageType.SERVICE_RESPONSE);
        return secureJsonCrypto.decrypt(
                encryptedResponse,
                tgsResponse.clientServiceSessionKey(),
                SecureAad.serviceResponse(request.requestId()),
                ServiceResponse.class);
    }

    private ClientAuthenticator authenticator(String authenticatorId, Instant issuedAt) {
        return new ClientAuthenticator(
                ProtocolDefaults.CURRENT_VERSION,
                authenticatorId,
                issuedAt,
                issuedAt.plus(config.replayWindow()),
                config.defaultClientId(),
                config.clientHost());
    }

    private ProtocolEnvelope envelope(MessageType messageType, String requestId, Object payload) {
        return new ProtocolEnvelope(
                messageType,
                ProtocolDefaults.CURRENT_VERSION,
                requestId,
                Instant.now(),
                codec.encodePayload(payload));
    }

    private CryptoEnvelope encryptedPayloadOrThrow(ProtocolEnvelope response, MessageType expectedType)
            throws AuthClientException {
        if (response.messageType() == MessageType.ERROR_RESPONSE) {
            throw new AuthClientException(codec.decodePayload(response.payloadJson(), ErrorResponse.class));
        }
        if (response.messageType() != expectedType) {
            ErrorResponse error = new ErrorResponse(
                    ProtocolDefaults.CURRENT_VERSION,
                    response.requestId(),
                    Instant.now(),
                    "auth-client-sdk",
                    "CLIENT_UNEXPECTED_MESSAGE",
                    "Tipo de respuesta inesperado: " + response.messageType(),
                    response.requestId());
            throw new AuthClientException(error);
        }
        return codec.decodePayload(response.payloadJson(), CryptoEnvelope.class);
    }
}
