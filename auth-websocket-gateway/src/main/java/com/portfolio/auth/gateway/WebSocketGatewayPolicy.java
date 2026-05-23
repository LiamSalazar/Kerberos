package com.portfolio.auth.gateway;

import java.time.Duration;
import java.util.Set;

public record WebSocketGatewayPolicy(
        Set<String> allowedOrigins,
        int maxMessagesPerConnection,
        Duration rateLimitWindow,
        Duration flowTimeout
) {
    public static final int DEFAULT_MAX_MESSAGES_PER_CONNECTION = 20;
    public static final Duration DEFAULT_RATE_LIMIT_WINDOW = Duration.ofSeconds(10);
    public static final Duration DEFAULT_FLOW_TIMEOUT = Duration.ofSeconds(15);

    public WebSocketGatewayPolicy {
        allowedOrigins = allowedOrigins == null ? Set.of() : Set.copyOf(allowedOrigins);
        if (maxMessagesPerConnection < 1) {
            throw new IllegalArgumentException("maxMessagesPerConnection must be >= 1");
        }
        if (rateLimitWindow == null || rateLimitWindow.isZero() || rateLimitWindow.isNegative()) {
            throw new IllegalArgumentException("rateLimitWindow must be positive");
        }
        if (flowTimeout == null || flowTimeout.isZero() || flowTimeout.isNegative()) {
            throw new IllegalArgumentException("flowTimeout must be positive");
        }
    }

    public static WebSocketGatewayPolicy defaults() {
        return new WebSocketGatewayPolicy(
                Set.of(),
                DEFAULT_MAX_MESSAGES_PER_CONNECTION,
                DEFAULT_RATE_LIMIT_WINDOW,
                DEFAULT_FLOW_TIMEOUT);
    }

    public boolean allowsOrigin(String origin) {
        if (allowedOrigins.isEmpty()) {
            return true;
        }
        return origin != null && allowedOrigins.contains(origin);
    }
}
