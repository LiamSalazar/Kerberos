package com.portfolio.auth.core.health;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class HealthCheckSupport {
    private final String service;
    private final String version;
    private final String storageMode;
    private final Instant startedAt;

    public HealthCheckSupport(String service, String version, String storageMode) {
        this.service = requireText(service, "service");
        this.version = requireText(version, "version");
        this.storageMode = requireText(storageMode, "storageMode");
        this.startedAt = Instant.now();
    }

    public HealthStatus status() {
        return new HealthStatus(
                "UP",
                service,
                version,
                Duration.between(startedAt, Instant.now()).toSeconds(),
                storageMode,
                Instant.now());
    }

    public String json() {
        HealthStatus status = status();
        return "{"
                + "\"status\":" + quote(status.status()) + ","
                + "\"service\":" + quote(status.service()) + ","
                + "\"version\":" + quote(status.version()) + ","
                + "\"uptimeSeconds\":" + status.uptimeSeconds() + ","
                + "\"storageMode\":" + quote(status.storageMode()) + ","
                + "\"timestamp\":" + quote(status.timestamp().toString())
                + "}";
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String quote(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}
