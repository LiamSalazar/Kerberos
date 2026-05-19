package com.portfolio.auth.gateway;

import java.time.Instant;

public record WebSocketAuthSession(
        String requestId,
        String clientId,
        String serviceId,
        Instant startedAt
) {
}
