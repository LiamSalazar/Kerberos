package com.portfolio.auth.transport.legacy;

import com.portfolio.auth.core.protocol.dto.ServiceResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class LegacyServiceResponseMapper {
    public static final String INCREMENTED_TIMESTAMP_KEY = "[TimeStamp-incrementada]";
    public static final String SERVICE_MESSAGE_KEY = "[Servicio]";
    public static final String ACCESS_GRANTED_KEY = "[Access-Granted]";
    public static final String SERVER_VALIDATED_AT_KEY = "[Server-Validated-At]";
    public static final String AUTHENTICATOR_ISSUED_AT_KEY = "[Authenticator-Issued-At]";

    private LegacyServiceResponseMapper() {
    }

    public static HashMap<String, Object> toLegacyMap(ServiceResponse response) {
        Objects.requireNonNull(response, "response");

        HashMap<String, Object> legacy = new HashMap<>();
        legacy.put(INCREMENTED_TIMESTAMP_KEY,
                LegacyMapperSupport.toLegacyDateTime(response.clientAuthenticatorIssuedAt().plusSeconds(60)));
        legacy.put(SERVICE_MESSAGE_KEY, response.serviceMessage());
        legacy.put(ACCESS_GRANTED_KEY, response.accessGranted());
        legacy.put(SERVER_VALIDATED_AT_KEY, response.serverValidatedAt());
        legacy.put(AUTHENTICATOR_ISSUED_AT_KEY, response.clientAuthenticatorIssuedAt());
        legacy.put(LegacyMapperSupport.CLIENT_ID_KEY, response.clientId());
        legacy.put(LegacyMapperSupport.SERVICE_ID_KEY, response.serviceId());
        legacy.put(LegacyMapperSupport.VERSION_KEY, response.version());
        legacy.put(LegacyMapperSupport.REQUEST_ID_KEY, response.requestId());
        legacy.put(LegacyMapperSupport.ISSUED_AT_KEY, response.issuedAt());
        legacy.put(LegacyMapperSupport.EXPIRES_AT_KEY, response.expiresAt());
        return legacy;
    }

    public static ServiceResponse fromLegacyMap(Map<String, Object> legacyPayload) {
        Objects.requireNonNull(legacyPayload, "legacyPayload");

        return new ServiceResponse(
                LegacyMapperSupport.version(legacyPayload),
                LegacyMapperSupport.requestId(legacyPayload),
                LegacyMapperSupport.instantFrom(legacyPayload.get(LegacyMapperSupport.ISSUED_AT_KEY),
                        LegacyMapperSupport.ISSUED_AT_KEY),
                LegacyMapperSupport.instantFrom(legacyPayload.get(LegacyMapperSupport.EXPIRES_AT_KEY),
                        LegacyMapperSupport.EXPIRES_AT_KEY),
                LegacyMapperSupport.stringOrNull(legacyPayload, LegacyMapperSupport.CLIENT_ID_KEY),
                LegacyMapperSupport.stringOrNull(legacyPayload, LegacyMapperSupport.SERVICE_ID_KEY),
                LegacyMapperSupport.instantFromAny(legacyPayload, AUTHENTICATOR_ISSUED_AT_KEY,
                        INCREMENTED_TIMESTAMP_KEY),
                LegacyMapperSupport.instantFrom(legacyPayload.get(SERVER_VALIDATED_AT_KEY), SERVER_VALIDATED_AT_KEY),
                LegacyMapperSupport.requireString(legacyPayload, SERVICE_MESSAGE_KEY),
                Boolean.TRUE.equals(legacyPayload.get(ACCESS_GRANTED_KEY)));
    }
}
