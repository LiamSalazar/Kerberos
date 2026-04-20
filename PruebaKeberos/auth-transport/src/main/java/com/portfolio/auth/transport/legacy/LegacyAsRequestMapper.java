package com.portfolio.auth.transport.legacy;

import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.AsRequest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador de compatibilidad entre el DTO nuevo y el payload legacy basado en
 * {@code HashMap<String, Object>}.
 */
public final class LegacyAsRequestMapper {
    public static final String CLIENT_ID_KEY = "[Id-c]";
    public static final String TGS_ID_KEY = "[Id-tgs]";
    public static final String TIMESTAMP_KEY = "[TimeStamp-1]";
    public static final String VERSION_KEY = "[Protocol-Version]";
    public static final String REQUEST_ID_KEY = "[Request-Id]";
    public static final String ISSUED_AT_KEY = "[Issued-At]";

    private LegacyAsRequestMapper() {
    }

    public static HashMap<String, Object> toLegacyMap(AsRequest request) {
        Objects.requireNonNull(request, "request");

        HashMap<String, Object> legacy = new HashMap<>();
        legacy.put(CLIENT_ID_KEY, request.clientId());
        legacy.put(TGS_ID_KEY, request.ticketGrantingServerId());
        legacy.put(TIMESTAMP_KEY, LocalDateTime.ofInstant(request.issuedAt(), ZoneId.systemDefault()));
        legacy.put(VERSION_KEY, request.version());
        legacy.put(REQUEST_ID_KEY, request.requestId());
        legacy.put(ISSUED_AT_KEY, request.issuedAt());
        return legacy;
    }

    public static AsRequest fromLegacyMap(Map<String, Object> legacyPayload) {
        Objects.requireNonNull(legacyPayload, "legacyPayload");

        return new AsRequest(
                stringOrDefault(legacyPayload.get(VERSION_KEY), ProtocolDefaults.CURRENT_VERSION),
                stringOrDefault(legacyPayload.get(REQUEST_ID_KEY), "legacy-" + UUID.randomUUID()),
                requireString(legacyPayload, CLIENT_ID_KEY),
                requireString(legacyPayload, TGS_ID_KEY),
                extractIssuedAt(legacyPayload));
    }

    private static String requireString(Map<String, Object> legacyPayload, String key) {
        Object value = legacyPayload.get(key);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        throw new IllegalArgumentException("Falta el campo obligatorio " + key);
    }

    private static String stringOrDefault(Object value, String defaultValue) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return defaultValue;
    }

    private static Instant extractIssuedAt(Map<String, Object> legacyPayload) {
        Object issuedAt = legacyPayload.get(ISSUED_AT_KEY);
        if (issuedAt instanceof Instant instant) {
            return instant;
        }

        Object timestamp = legacyPayload.get(TIMESTAMP_KEY);
        if (timestamp instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        }

        throw new IllegalArgumentException("Falta el campo temporal " + TIMESTAMP_KEY);
    }
}
