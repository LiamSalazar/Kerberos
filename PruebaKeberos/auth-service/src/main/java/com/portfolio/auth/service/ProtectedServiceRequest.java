package com.portfolio.auth.service;

import java.time.Instant;

public record ProtectedServiceRequest(
        String requestId,
        String clientId,
        String serviceId,
        Instant authenticatedAt
) {
}
