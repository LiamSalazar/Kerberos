package com.portfolio.auth.core.config;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;

/**
 * Configuracion central para la ruta modular local.
 *
 * Los valores por defecto son secretos de demostracion para ejecucion local.
 * En modo strict deben reemplazarse por variables de entorno explicitas.
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
        String demoClientSecret,
        String demoClientTgsSessionKey,
        String demoTicketGrantingServerSecret,
        String demoClientServiceSessionKey,
        String demoServiceSecret,
        String demoPbkdf2Salt
) implements Serializable {
    public static final String DEFAULT_LOCAL_CLIENT_ID = "1";
    public static final String DEFAULT_LOCAL_TGS_ID = "1";
    public static final String DEFAULT_LOCAL_SERVICE_ID = "1";
    public static final String DEFAULT_LOCAL_HOST = "127.0.0.1";
    public static final int DEFAULT_LOCAL_AS_PORT = 2000;
    public static final int DEFAULT_LOCAL_TGS_PORT = 2001;
    public static final int DEFAULT_LOCAL_SERVICE_PORT = 2002;
    public static final Duration DEFAULT_LOCAL_TICKET_LIFETIME = Duration.ofMinutes(5);
    public static final Duration DEFAULT_LOCAL_ALLOWED_CLOCK_SKEW = Duration.ofMinutes(2);
    public static final Duration DEFAULT_LOCAL_REPLAY_WINDOW = Duration.ofMinutes(5);
    public static final String DEFAULT_LOCAL_DEMO_CLIENT_SECRET = "ContraseniaCliente";
    public static final String DEFAULT_LOCAL_DEMO_CLIENT_TGS_KEY = "contraseña_C-TGS";
    public static final String DEFAULT_LOCAL_DEMO_TGS_SECRET = "contraseñaTGS";
    public static final String DEFAULT_LOCAL_DEMO_CLIENT_SERVICE_KEY = "contraseñaClienteServidor";
    public static final String DEFAULT_LOCAL_DEMO_SERVICE_SECRET = "contraseñaServidor";
    public static final String DEFAULT_LOCAL_DEMO_PBKDF2_SALT = "12345678";

    public static final String MODE_DEMO = "demo";
    public static final String MODE_LOCAL = "local";
    public static final String MODE_STRICT = "strict";

    public static final String ENV_AUTH_MODE = "AUTH_MODE";
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
    public static final String ENV_DEMO_CLIENT_SECRET = "AUTH_DEMO_CLIENT_SECRET";
    public static final String ENV_DEMO_CLIENT_TGS_KEY = "AUTH_DEMO_CLIENT_TGS_KEY";
    public static final String ENV_DEMO_TGS_SECRET = "AUTH_DEMO_TGS_SECRET";
    public static final String ENV_DEMO_CLIENT_SERVICE_KEY = "AUTH_DEMO_CLIENT_SERVICE_KEY";
    public static final String ENV_DEMO_SERVICE_SECRET = "AUTH_DEMO_SERVICE_SECRET";
    public static final String ENV_DEMO_PBKDF2_SALT = "AUTH_DEMO_PBKDF2_SALT";

    public static AuthConfig localDemo() {
        return new AuthConfig(
                DEFAULT_LOCAL_CLIENT_ID,
                DEFAULT_LOCAL_TGS_ID,
                DEFAULT_LOCAL_SERVICE_ID,
                DEFAULT_LOCAL_HOST,
                DEFAULT_LOCAL_HOST,
                DEFAULT_LOCAL_HOST,
                DEFAULT_LOCAL_HOST,
                DEFAULT_LOCAL_AS_PORT,
                DEFAULT_LOCAL_TGS_PORT,
                DEFAULT_LOCAL_SERVICE_PORT,
                DEFAULT_LOCAL_TICKET_LIFETIME,
                DEFAULT_LOCAL_ALLOWED_CLOCK_SKEW,
                DEFAULT_LOCAL_REPLAY_WINDOW,
                DEFAULT_LOCAL_DEMO_CLIENT_SECRET,
                DEFAULT_LOCAL_DEMO_CLIENT_TGS_KEY,
                DEFAULT_LOCAL_DEMO_TGS_SECRET,
                DEFAULT_LOCAL_DEMO_CLIENT_SERVICE_KEY,
                DEFAULT_LOCAL_DEMO_SERVICE_SECRET,
                DEFAULT_LOCAL_DEMO_PBKDF2_SALT);
    }

    public static AuthConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static AuthConfig fromEnvironment(Map<String, String> environment) {
        AuthConfig defaults = localDemo();

        AuthConfig config = new AuthConfig(
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
                value(environment, ENV_DEMO_CLIENT_SECRET, defaults.demoClientSecret()),
                value(environment, ENV_DEMO_CLIENT_TGS_KEY, defaults.demoClientTgsSessionKey()),
                value(environment, ENV_DEMO_TGS_SECRET, defaults.demoTicketGrantingServerSecret()),
                value(environment, ENV_DEMO_CLIENT_SERVICE_KEY, defaults.demoClientServiceSessionKey()),
                value(environment, ENV_DEMO_SERVICE_SECRET, defaults.demoServiceSecret()),
                value(environment, ENV_DEMO_PBKDF2_SALT, defaults.demoPbkdf2Salt()));

        if (isStrictMode(environment)) {
            validateStrictMode(environment, config);
        }
        return config;
    }

    public static boolean isStrictMode(Map<String, String> environment) {
        return MODE_STRICT.equals(mode(environment));
    }

    public static String mode(Map<String, String> environment) {
        String configured = value(environment, ENV_AUTH_MODE, MODE_DEMO).trim().toLowerCase();
        if (MODE_DEMO.equals(configured) || MODE_LOCAL.equals(configured) || MODE_STRICT.equals(configured)) {
            return configured;
        }
        throw new IllegalArgumentException(
                ENV_AUTH_MODE + " debe ser '" + MODE_DEMO + "', '" + MODE_LOCAL + "' o '" + MODE_STRICT + "'");
    }

    public boolean usesDemoSecrets() {
        return DEFAULT_LOCAL_DEMO_CLIENT_SECRET.equals(demoClientSecret())
                || DEFAULT_LOCAL_DEMO_CLIENT_TGS_KEY.equals(demoClientTgsSessionKey())
                || DEFAULT_LOCAL_DEMO_TGS_SECRET.equals(demoTicketGrantingServerSecret())
                || DEFAULT_LOCAL_DEMO_CLIENT_SERVICE_KEY.equals(demoClientServiceSessionKey())
                || DEFAULT_LOCAL_DEMO_SERVICE_SECRET.equals(demoServiceSecret())
                || DEFAULT_LOCAL_DEMO_PBKDF2_SALT.equals(demoPbkdf2Salt());
    }

    private static void validateStrictMode(Map<String, String> environment, AuthConfig config) {
        StringBuilder missing = new StringBuilder();
        requireSecret(environment, config.demoClientSecret(), ENV_DEMO_CLIENT_SECRET,
                DEFAULT_LOCAL_DEMO_CLIENT_SECRET, missing);
        requireSecret(environment, config.demoClientTgsSessionKey(), ENV_DEMO_CLIENT_TGS_KEY,
                DEFAULT_LOCAL_DEMO_CLIENT_TGS_KEY, missing);
        requireSecret(environment, config.demoTicketGrantingServerSecret(), ENV_DEMO_TGS_SECRET,
                DEFAULT_LOCAL_DEMO_TGS_SECRET, missing);
        requireSecret(environment, config.demoClientServiceSessionKey(), ENV_DEMO_CLIENT_SERVICE_KEY,
                DEFAULT_LOCAL_DEMO_CLIENT_SERVICE_KEY, missing);
        requireSecret(environment, config.demoServiceSecret(), ENV_DEMO_SERVICE_SECRET,
                DEFAULT_LOCAL_DEMO_SERVICE_SECRET, missing);
        requireSecret(environment, config.demoPbkdf2Salt(), ENV_DEMO_PBKDF2_SALT,
                DEFAULT_LOCAL_DEMO_PBKDF2_SALT, missing);

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    ENV_AUTH_MODE + "=" + MODE_STRICT + " requiere secretos explicitos no-default: " + missing);
        }
    }

    private static void requireSecret(
            Map<String, String> environment,
            String resolvedValue,
            String key,
            String defaultValue,
            StringBuilder missing) {
        String configured = environment.get(key);
        if (configured == null || configured.isBlank() || defaultValue.equals(resolvedValue)) {
            if (!missing.isEmpty()) {
                missing.append(", ");
            }
            missing.append(key);
        }
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
