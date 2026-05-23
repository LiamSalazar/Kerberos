package com.portfolio.auth.gateway;

import com.portfolio.auth.core.protocol.dto.ServiceResponse;
import com.portfolio.auth.transport.secure.SecureAsResponse;
import com.portfolio.auth.transport.secure.SecureTgsResponse;

import java.time.Instant;

public interface GatewayAuthClient {
    String configuredClientId();

    SecureAsResponse requestTicketGrantingTicket(String requestId) throws Exception;

    SecureTgsResponse requestServiceTicket(
            SecureAsResponse asResponse,
            String serviceId,
            String requestId,
            String authenticatorId,
            Instant authenticatorIssuedAt) throws Exception;

    ServiceResponse requestProtectedService(
            SecureTgsResponse tgsResponse,
            String requestId,
            String authenticatorId,
            Instant authenticatorIssuedAt) throws Exception;
}
