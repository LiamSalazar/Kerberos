package com.portfolio.auth.gateway;

import com.portfolio.auth.client.AuthClient;
import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.dto.ServiceResponse;
import com.portfolio.auth.transport.secure.SecureAsResponse;
import com.portfolio.auth.transport.secure.SecureTgsResponse;

import java.time.Instant;
import java.util.Objects;

public final class DefaultGatewayAuthClient implements GatewayAuthClient {
    private final AuthConfig config;
    private final AuthClient authClient;

    public DefaultGatewayAuthClient(AuthConfig config, AuthClient authClient) {
        this.config = Objects.requireNonNull(config, "config");
        this.authClient = Objects.requireNonNull(authClient, "authClient");
    }

    @Override
    public String configuredClientId() {
        return config.defaultClientId();
    }

    @Override
    public SecureAsResponse requestTicketGrantingTicket(String requestId) throws Exception {
        return authClient.requestTicketGrantingTicket(requestId);
    }

    @Override
    public SecureTgsResponse requestServiceTicket(
            SecureAsResponse asResponse,
            String serviceId,
            String requestId,
            String authenticatorId,
            Instant authenticatorIssuedAt) throws Exception {
        return authClient.requestServiceTicket(asResponse, serviceId, requestId, authenticatorId, authenticatorIssuedAt);
    }

    @Override
    public ServiceResponse requestProtectedService(
            SecureTgsResponse tgsResponse,
            String requestId,
            String authenticatorId,
            Instant authenticatorIssuedAt) throws Exception {
        return authClient.requestProtectedService(tgsResponse, requestId, authenticatorId, authenticatorIssuedAt);
    }
}
