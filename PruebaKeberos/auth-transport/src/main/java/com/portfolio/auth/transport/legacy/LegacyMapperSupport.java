package com.portfolio.auth.transport.legacy;

import com.portfolio.auth.core.protocol.ProtocolDefaults;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

final class LegacyMapperSupport {
    static final String VERSION_KEY = "[Protocol-Version]";
    static final String REQUEST_ID_KEY = "[Request-Id]";
    static final String ISSUED_AT_KEY = "[Issued-At]";
    static final String EXPIRES_AT_KEY = "[Expires-At]";
    static final String CLIENT_ID_KEY = "[Id-c]";
    static final String TGS_ID_KEY = "[Id-tgs]";
    static final String SERVICE_ID_KEY = "[Id-v]";
    static final String CLIENT_ADDRESS_KEY = "[Client-Address]";

    private LegacyMapperSupport() {
    }

    static LocalDateTime toLegacyDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    static Instant instantFrom(Object value, String key) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        }
        throw new IllegalArgumentException("Falta el campo temporal " + key);
    }

    static Instant instantOrNull(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        return instantFrom(value, key);
    }

    static Instant instantFromAny(Map<String, Object> payload, String primaryKey, String fallbackKey) {
        Object primaryValue = payload.get(primaryKey);
        if (primaryValue != null) {
            return instantFrom(primaryValue, primaryKey);
        }
        return instantFrom(payload.get(fallbackKey), fallbackKey);
    }

    static String requireString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        throw new IllegalArgumentException("Falta el campo obligatorio " + key);
    }

    static String stringOrNull(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return null;
    }

    static String stringOrDefault(Map<String, Object> payload, String key, String defaultValue) {
        String value = stringOrNull(payload, key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    static String version(Map<String, Object> payload) {
        return stringOrDefault(payload, VERSION_KEY, ProtocolDefaults.CURRENT_VERSION);
    }

    static String requestId(Map<String, Object> payload) {
        return stringOrDefault(payload, REQUEST_ID_KEY, "legacy-" + UUID.randomUUID());
    }

    static <T> T typedOrNull(Map<String, Object> payload, String key, Class<T> type) {
        Object value = payload.get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
}
