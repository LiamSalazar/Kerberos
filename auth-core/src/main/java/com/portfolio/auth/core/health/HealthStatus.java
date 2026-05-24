package com.portfolio.auth.core.health;

import java.time.Instant;
import java.util.Objects;

public record HealthStatus(
        String status,
        String service,
        String version,
        long uptimeSeconds,
        String storageMode,
        Instant timestamp
) {
    public HealthStatus {
        status = requireText(status, "status");
        service = requireText(service, "service");
        version = requireText(version, "version");
        storageMode = requireText(storageMode, "storageMode");
        timestamp = Objects.requireNonNull(timestamp, "timestamp");
        if (uptimeSeconds < 0) {
            throw new IllegalArgumentException("uptimeSeconds must be >= 0");
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
