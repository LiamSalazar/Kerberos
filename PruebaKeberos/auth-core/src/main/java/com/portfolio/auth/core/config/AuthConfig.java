package com.portfolio.auth.core.config;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;

/**
 * Configuracion central para la demo local y la migracion modular.
 *
 * Los valores por defecto preservan compatibilidad con el flujo legacy. No son
 * secretos de produccion.
 */
public record AuthConfig(
        String defaultClientId,
        String defaultTicketGrantingServerId,
        String defaultServiceId,
        String clientHost,
        String authenticationServerHost,
        String ticketGrantingServerHost,
        String serviceServerHost,
        int authenticationServerPort,
        int ticketGrantingServerPort,
        int serviceServerPort,
        Duration ticketLifetime,
        Duration allowedClockSkew,
        Duration replayWindow,
        String legacyClientSecret,
        String legacyClientTgsSessionKey,
        String legacyTicketGrantingServerSecret,
        String legacyClientServiceSessionKey,
        String legacyServiceSecret,
        String legacyPbkdf2Salt
) implements Serializable {
    public static final String ENV_CLIENT_ID = "AUTH_DEMO_CLIENT_ID";
    public static final String ENV_TGS_ID = "AUTH_DEMO_TGS_ID";
    public static final String ENV_SERVICE_ID = "AUTH_DEMO_SERVICE_ID";
    public static final String ENV_CLIENT_HOST = "AUTH_DEMO_CLIENT_HOST";
    public static final String ENV_AS_HOST = "AUTH_AS_HOST";
    public static final String ENV_TGS_HOST = "AUTH_TGS_HOST";
    public static final String ENV_SERVICE_HOST = "AUTH_SERVICE_HOST";
    public static final String ENV_AS_PORT = "AUTH_AS_PORT";
    public static final String ENV_TGS_PORT = "AUTH_TGS_PORT";
    public static final String ENV_SERVICE_PORT = "AUTH_SERVICE_PORT";
    public static final String ENV_TICKET_TTL_MINUTES = "AUTH_TICKET_TTL_MINUTES";
    public static final String ENV_ALLOWED_SKEW_SECONDS = "AUTH_ALLOWED_SKEW_SECONDS";
    public static final String ENV_REPLAY_WINDOW_SECONDS = "AUTH_REPLAY_WINDOW_SECONDS";
    public static final String ENV_LEGACY_CLIENT_SECRET = "AUTH_LEGACY_CLIENT_SECRET";
    public static final String ENV_LEGACY_CLIENT_TGS_KEY = "AUTH_LEGACY_CLIENT_TGS_KEY";
    public static final String ENV_LEGACY_TGS_SECRET = "AUTH_LEGACY_TGS_SECRET";
    public static final String ENV_LEGACY_CLIENT_SERVICE_KEY = "AUTH_LEGACY_CLIENT_SERVICE_KEY";
    public static final String ENV_LEGACY_SERVICE_SECRET = "AUTH_LEGACY_SERVICE_SECRET";
    public static final String ENV_LEGACY_PBKDF2_SALT = "AUTH_LEGACY_PBKDF2_SALT";

    public static AuthConfig localDemo() {
        return new AuthConfig(
                "1",
                "1",
                "1",
                "127.0.0.1",
                "127.0.0.1",
                "127.0.0.1",
                "127.0.0.1",
                2000,
                2001,
                2002,
                Duration.ofMinutes(5),
                Duration.ofMinutes(2),
                Duration.ofMinutes(5),
                "ContraseniaCliente",
                "contraseña_C-TGS",
                "contraseñaTGS",
                "contraseñaClienteServidor",
                "contraseñaServidor",
                "12345678");
    }

    public static AuthConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static AuthConfig fromEnvironment(Map<String, String> environment) {
        AuthConfig defaults = localDemo();

        return new AuthConfig(
                value(environment, ENV_CLIENT_ID, defaults.defaultClientId()),
                value(environment, ENV_TGS_ID, defaults.defaultTicketGrantingServerId()),
                value(environment, ENV_SERVICE_ID, defaults.defaultServiceId()),
                value(environment, ENV_CLIENT_HOST, defaults.clientHost()),
                value(environment, ENV_AS_HOST, defaults.authenticationServerHost()),
                value(environment, ENV_TGS_HOST, defaults.ticketGrantingServerHost()),
                value(environment, ENV_SERVICE_HOST, defaults.serviceServerHost()),
                intValue(environment, ENV_AS_PORT, defaults.authenticationServerPort()),
                intValue(environment, ENV_TGS_PORT, defaults.ticketGrantingServerPort()),
                intValue(environment, ENV_SERVICE_PORT, defaults.serviceServerPort()),
                Duration.ofMinutes(longValue(environment, ENV_TICKET_TTL_MINUTES, defaults.ticketLifetime().toMinutes())),
                Duration.ofSeconds(longValue(environment, ENV_ALLOWED_SKEW_SECONDS, defaults.allowedClockSkew().toSeconds())),
                Duration.ofSeconds(longValue(environment, ENV_REPLAY_WINDOW_SECONDS, defaults.replayWindow().toSeconds())),
                value(environment, ENV_LEGACY_CLIENT_SECRET, defaults.legacyClientSecret()),
                value(environment, ENV_LEGACY_CLIENT_TGS_KEY, defaults.legacyClientTgsSessionKey()),
                value(environment, ENV_LEGACY_TGS_SECRET, defaults.legacyTicketGrantingServerSecret()),
                value(environment, ENV_LEGACY_CLIENT_SERVICE_KEY, defaults.legacyClientServiceSessionKey()),
                value(environment, ENV_LEGACY_SERVICE_SECRET, defaults.legacyServiceSecret()),
                value(environment, ENV_LEGACY_PBKDF2_SALT, defaults.legacyPbkdf2Salt()));
    }

    private static String value(Map<String, String> environment, String key, String defaultValue) {
        String configured = environment.get(key);
        if (configured == null || configured.isBlank()) {
            return defaultValue;
        }
        return configured;
    }

    private static int intValue(Map<String, String> environment, String key, int defaultValue) {
        return Math.toIntExact(longValue(environment, key, defaultValue));
    }

    private static long longValue(Map<String, String> environment, String key, long defaultValue) {
        String configured = environment.get(key);
        if (configured == null || configured.isBlank()) {
            return defaultValue;
        }

        try {
            return Long.parseLong(configured);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
