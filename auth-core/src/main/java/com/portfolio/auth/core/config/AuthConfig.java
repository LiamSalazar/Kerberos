package com.portfolio.auth.core.config;

import com.portfolio.auth.core.secrets.AwsSecretsManagerProvider;
import com.portfolio.auth.core.secrets.EnvSecretsProvider;
import com.portfolio.auth.core.secrets.SecretRef;
import com.portfolio.auth.core.secrets.SecretResolutionException;
import com.portfolio.auth.core.secrets.SecretsProvider;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Configuracion central para la ruta modular local.
 *
 * Los valores por defecto son secretos de demostracion para ejecucion local.
 * En modo strict deben reemplazarse por valores explicitos o referencias a un
 * proveedor de secretos.
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
        String demoTicketGrantingServerSecret,
        String demoServiceSecret,
        String demoPbkdf2Salt,
        String storageMode,
        String sqlitePath,
        String postgresUrl,
        String postgresUser,
        String postgresPassword,
        String postgresSslMode,
        Duration sessionTtl,
        Duration sessionMaxTtl,
        String sessionStorageMode,
        boolean requireSessionVerify,
        String secretProvider,
        String awsRegion,
        String clientSecretRef,
        String ticketGrantingServerSecretRef,
        String serviceSecretRef,
        String postgresPasswordRef
) implements Serializable {
    public AuthConfig {
        Objects.requireNonNull(sessionTtl, "sessionTtl");
        Objects.requireNonNull(sessionMaxTtl, "sessionMaxTtl");
        if (sessionTtl.isZero() || sessionTtl.isNegative()) {
            throw new IllegalArgumentException("sessionTtl must be positive");
        }
        if (sessionMaxTtl.isZero() || sessionMaxTtl.isNegative()) {
            throw new IllegalArgumentException("sessionMaxTtl must be positive");
        }
    }

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
    public static final String DEFAULT_LOCAL_DEMO_TGS_SECRET = "contraseñaTGS";
    public static final String DEFAULT_LOCAL_DEMO_SERVICE_SECRET = "contraseñaServidor";
    public static final String DEFAULT_LOCAL_DEMO_PBKDF2_SALT = "12345678";
    public static final String DEFAULT_LOCAL_STORAGE_MODE = "memory";
    public static final String DEFAULT_LOCAL_SQLITE_PATH = "data/auth-demo.sqlite";
    public static final String DEFAULT_LOCAL_POSTGRES_URL = "jdbc:postgresql://localhost:5432/kerberos_auth";
    public static final String DEFAULT_LOCAL_POSTGRES_USER = "kerberos_demo";
    public static final String DEFAULT_LOCAL_POSTGRES_PASSWORD = "";
    public static final String DEFAULT_LOCAL_POSTGRES_SSL_MODE = "prefer";
    public static final Duration DEFAULT_LOCAL_SESSION_TTL = Duration.ofMinutes(5);
    public static final Duration DEFAULT_LOCAL_SESSION_MAX_TTL = Duration.ofMinutes(5);
    public static final String DEFAULT_AWS_REGION = "us-east-1";

    public static final String MODE_DEMO = "demo";
    public static final String MODE_STRICT = "strict";
    public static final String STORAGE_MODE_MEMORY = "memory";
    public static final String STORAGE_MODE_SQLITE = "sqlite";
    public static final String STORAGE_MODE_POSTGRES = "postgres";
    public static final String SECRET_PROVIDER_ENV = "env";
    public static final String SECRET_PROVIDER_AWS_SECRETS_MANAGER = "aws-secrets-manager";

    public static final String ENV_AUTH_MODE = "AUTH_MODE";
    public static final String ENV_STORAGE_MODE = "AUTH_STORAGE_MODE";
    public static final String ENV_SQLITE_PATH = "AUTH_SQLITE_PATH";
    public static final String ENV_POSTGRES_URL = "AUTH_POSTGRES_URL";
    public static final String ENV_POSTGRES_USER = "AUTH_POSTGRES_USER";
    public static final String ENV_POSTGRES_PASSWORD = "AUTH_POSTGRES_PASSWORD";
    public static final String ENV_POSTGRES_SSL_MODE = "AUTH_POSTGRES_SSL_MODE";
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
    public static final String ENV_DEMO_TGS_SECRET = "AUTH_DEMO_TGS_SECRET";
    public static final String ENV_DEMO_SERVICE_SECRET = "AUTH_DEMO_SERVICE_SECRET";
    public static final String ENV_DEMO_PBKDF2_SALT = "AUTH_DEMO_PBKDF2_SALT";
    public static final String ENV_SESSION_TTL_SECONDS = "AUTH_SESSION_TTL_SECONDS";
    public static final String ENV_SESSION_MAX_TTL_SECONDS = "AUTH_SESSION_MAX_TTL_SECONDS";
    public static final String ENV_SESSION_STORAGE_MODE = "AUTH_SESSION_STORAGE_MODE";
    public static final String ENV_REQUIRE_SESSION_VERIFY = "AUTH_REQUIRE_SESSION_VERIFY";
    public static final String ENV_SECRET_PROVIDER = "AUTH_SECRET_PROVIDER";
    public static final String ENV_AWS_REGION = "AUTH_AWS_REGION";
    public static final String ENV_SECRET_CLIENT_SECRET_ID = "AUTH_SECRET_CLIENT_SECRET_ID";
    public static final String ENV_SECRET_TGS_SECRET_ID = "AUTH_SECRET_TGS_SECRET_ID";
    public static final String ENV_SECRET_SERVICE_SECRET_ID = "AUTH_SECRET_SERVICE_SECRET_ID";
    public static final String ENV_SECRET_POSTGRES_PASSWORD_ID = "AUTH_SECRET_POSTGRES_PASSWORD_ID";

    private static final String DEMO_WARNING =
            "[auth-config] AUTH_MODE=demo permite secretos demo por defecto; no usar en produccion critica.";

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
                DEFAULT_LOCAL_DEMO_TGS_SECRET,
                DEFAULT_LOCAL_DEMO_SERVICE_SECRET,
                DEFAULT_LOCAL_DEMO_PBKDF2_SALT,
                DEFAULT_LOCAL_STORAGE_MODE,
                DEFAULT_LOCAL_SQLITE_PATH,
                DEFAULT_LOCAL_POSTGRES_URL,
                DEFAULT_LOCAL_POSTGRES_USER,
                DEFAULT_LOCAL_POSTGRES_PASSWORD,
                DEFAULT_LOCAL_POSTGRES_SSL_MODE,
                DEFAULT_LOCAL_SESSION_TTL,
                DEFAULT_LOCAL_SESSION_MAX_TTL,
                DEFAULT_LOCAL_STORAGE_MODE,
                true,
                SECRET_PROVIDER_ENV,
                DEFAULT_AWS_REGION,
                "",
                "",
                "",
                "");
    }

    public static AuthConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static AuthConfig fromEnvironment(Map<String, String> environment) {
        return fromEnvironment(environment, secretsProvider(environment));
    }

    public static AuthConfig fromEnvironment(Map<String, String> environment, SecretsProvider secretsProvider) {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(secretsProvider, "secretsProvider");
        AuthConfig defaults = localDemo();

        boolean strict = isStrictMode(environment);
        String resolvedSecretProvider = secretProviderMode(environment);
        String resolvedStorageMode = storageMode(environment);
        String resolvedSessionStorageMode = sessionStorageMode(environment, resolvedStorageMode);

        String clientSecret = resolveRuntimeSecret(
                environment,
                secretsProvider,
                resolvedSecretProvider,
                ENV_DEMO_CLIENT_SECRET,
                ENV_SECRET_CLIENT_SECRET_ID,
                defaults.demoClientSecret(),
                strict);
        String tgsSecret = resolveRuntimeSecret(
                environment,
                secretsProvider,
                resolvedSecretProvider,
                ENV_DEMO_TGS_SECRET,
                ENV_SECRET_TGS_SECRET_ID,
                defaults.demoTicketGrantingServerSecret(),
                strict);
        String serviceSecret = resolveRuntimeSecret(
                environment,
                secretsProvider,
                resolvedSecretProvider,
                ENV_DEMO_SERVICE_SECRET,
                ENV_SECRET_SERVICE_SECRET_ID,
                defaults.demoServiceSecret(),
                strict);
        String postgresPassword = resolvePostgresPassword(
                environment,
                secretsProvider,
                resolvedSecretProvider,
                strict,
                STORAGE_MODE_POSTGRES.equals(resolvedStorageMode)
                        || STORAGE_MODE_POSTGRES.equals(resolvedSessionStorageMode));

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
                clientSecret,
                tgsSecret,
                serviceSecret,
                value(environment, ENV_DEMO_PBKDF2_SALT, defaults.demoPbkdf2Salt()),
                resolvedStorageMode,
                value(environment, ENV_SQLITE_PATH, defaults.sqlitePath()),
                value(environment, ENV_POSTGRES_URL, defaults.postgresUrl()),
                value(environment, ENV_POSTGRES_USER, defaults.postgresUser()),
                postgresPassword,
                value(environment, ENV_POSTGRES_SSL_MODE, defaults.postgresSslMode()),
                Duration.ofSeconds(longValue(environment, ENV_SESSION_TTL_SECONDS,
                        defaults.sessionTtl().toSeconds())),
                Duration.ofSeconds(longValue(environment, ENV_SESSION_MAX_TTL_SECONDS,
                        defaults.sessionMaxTtl().toSeconds())),
                resolvedSessionStorageMode,
                requireSessionVerify(environment),
                resolvedSecretProvider,
                value(environment, ENV_AWS_REGION, defaults.awsRegion()),
                value(environment, ENV_SECRET_CLIENT_SECRET_ID, ""),
                value(environment, ENV_SECRET_TGS_SECRET_ID, ""),
                value(environment, ENV_SECRET_SERVICE_SECRET_ID, ""),
                value(environment, ENV_SECRET_POSTGRES_PASSWORD_ID, ""));

        if (strict) {
            validateStrictMode(environment, config);
        }
        return config;
    }

    public static boolean isStrictMode(Map<String, String> environment) {
        return MODE_STRICT.equals(mode(environment));
    }

    public static String demoWarning(Map<String, String> environment, AuthConfig config) {
        if (MODE_DEMO.equals(mode(environment)) && config.usesDemoSecrets()) {
            return DEMO_WARNING;
        }
        return "";
    }

    public static void printDemoWarningIfNeeded(Map<String, String> environment, AuthConfig config) {
        String warning = demoWarning(environment, config);
        if (!warning.isBlank()) {
            System.err.println(warning);
        }
    }

    public static String mode(Map<String, String> environment) {
        String configured = value(environment, ENV_AUTH_MODE, MODE_DEMO).trim().toLowerCase();
        if (MODE_DEMO.equals(configured) || MODE_STRICT.equals(configured)) {
            return configured;
        }
        throw new IllegalArgumentException(
                ENV_AUTH_MODE + " debe ser '" + MODE_DEMO + "' o '" + MODE_STRICT + "'");
    }

    public static String storageMode(Map<String, String> environment) {
        String configured = value(environment, ENV_STORAGE_MODE, DEFAULT_LOCAL_STORAGE_MODE).trim().toLowerCase();
        if (isSupportedStorageMode(configured)) {
            return configured;
        }
        throw new IllegalArgumentException(ENV_STORAGE_MODE + " debe ser '" + STORAGE_MODE_MEMORY + "', '"
                + STORAGE_MODE_SQLITE + "' o '" + STORAGE_MODE_POSTGRES + "'");
    }

    public static String sessionStorageMode(Map<String, String> environment, String resolvedStorageMode) {
        String configured = value(environment, ENV_SESSION_STORAGE_MODE, resolvedStorageMode).trim().toLowerCase();
        if (isSupportedStorageMode(configured)) {
            return configured;
        }
        throw new IllegalArgumentException(ENV_SESSION_STORAGE_MODE + " debe ser '" + STORAGE_MODE_MEMORY + "', '"
                + STORAGE_MODE_SQLITE + "' o '" + STORAGE_MODE_POSTGRES + "'");
    }

    public static String secretProviderMode(Map<String, String> environment) {
        String configured = value(environment, ENV_SECRET_PROVIDER, SECRET_PROVIDER_ENV).trim().toLowerCase();
        if (SECRET_PROVIDER_ENV.equals(configured) || SECRET_PROVIDER_AWS_SECRETS_MANAGER.equals(configured)) {
            return configured;
        }
        throw new IllegalArgumentException(ENV_SECRET_PROVIDER + " debe ser '" + SECRET_PROVIDER_ENV + "' o '"
                + SECRET_PROVIDER_AWS_SECRETS_MANAGER + "'");
    }

    public static boolean requireSessionVerify(Map<String, String> environment) {
        if (isStrictMode(environment)) {
            return true;
        }
        return booleanValue(environment, ENV_REQUIRE_SESSION_VERIFY, true);
    }

    public boolean usesSqliteStorage() {
        return STORAGE_MODE_SQLITE.equals(storageMode());
    }

    public boolean usesPostgresStorage() {
        return STORAGE_MODE_POSTGRES.equals(storageMode());
    }

    public boolean usesSqliteSessionStorage() {
        return STORAGE_MODE_SQLITE.equals(sessionStorageMode());
    }

    public boolean usesPostgresSessionStorage() {
        return STORAGE_MODE_POSTGRES.equals(sessionStorageMode());
    }

    public boolean usesPersistentSessionStorage() {
        return usesSqliteSessionStorage() || usesPostgresSessionStorage();
    }

    public boolean usesDemoSecrets() {
        return DEFAULT_LOCAL_DEMO_CLIENT_SECRET.equals(demoClientSecret())
                || DEFAULT_LOCAL_DEMO_TGS_SECRET.equals(demoTicketGrantingServerSecret())
                || DEFAULT_LOCAL_DEMO_SERVICE_SECRET.equals(demoServiceSecret());
    }

    private static SecretsProvider secretsProvider(Map<String, String> environment) {
        String mode = secretProviderMode(environment);
        if (SECRET_PROVIDER_AWS_SECRETS_MANAGER.equals(mode)) {
            return new AwsSecretsManagerProvider(value(environment, ENV_AWS_REGION, DEFAULT_AWS_REGION));
        }
        return new EnvSecretsProvider(environment);
    }

    private static String resolveRuntimeSecret(
            Map<String, String> environment,
            SecretsProvider provider,
            String providerMode,
            String directEnv,
            String refEnv,
            String defaultValue,
            boolean strict) {
        String directValue = environment.get(directEnv);
        if (SECRET_PROVIDER_ENV.equals(providerMode) && directValue != null && !directValue.isBlank()) {
            return directValue;
        }

        String secretId = environment.get(refEnv);
        if (secretId != null && !secretId.isBlank()) {
            return provider.resolveRequired(secretRef(providerMode, secretId));
        }

        if (!SECRET_PROVIDER_ENV.equals(providerMode) && !strict && directValue != null && !directValue.isBlank()) {
            return directValue;
        }

        if (strict) {
            throw new SecretResolutionException(
                    ENV_AUTH_MODE + "=" + MODE_STRICT + " requiere secreto explicito: " + directEnv
                            + " o " + refEnv);
        }
        return defaultValue;
    }

    private static String resolvePostgresPassword(
            Map<String, String> environment,
            SecretsProvider provider,
            String providerMode,
            boolean strict,
            boolean postgresRequired) {
        String directValue = environment.get(ENV_POSTGRES_PASSWORD);
        if (SECRET_PROVIDER_ENV.equals(providerMode) && directValue != null && !directValue.isBlank()) {
            return directValue;
        }

        String secretId = environment.get(ENV_SECRET_POSTGRES_PASSWORD_ID);
        if (secretId != null && !secretId.isBlank()) {
            return provider.resolveRequired(secretRef(providerMode, secretId));
        }

        if (!SECRET_PROVIDER_ENV.equals(providerMode) && !strict && directValue != null && !directValue.isBlank()) {
            return directValue;
        }

        if (strict && postgresRequired) {
            throw new SecretResolutionException(
                    ENV_AUTH_MODE + "=" + MODE_STRICT + " con postgres requiere " + ENV_POSTGRES_PASSWORD
                            + " o " + ENV_SECRET_POSTGRES_PASSWORD_ID);
        }
        return DEFAULT_LOCAL_POSTGRES_PASSWORD;
    }

    private static SecretRef secretRef(String providerMode, String id) {
        if (SECRET_PROVIDER_AWS_SECRETS_MANAGER.equals(providerMode)) {
            return SecretRef.awsSecretsManager(id);
        }
        return SecretRef.env(id);
    }

    private static void validateStrictMode(Map<String, String> environment, AuthConfig config) {
        if (config.usesDemoSecrets()) {
            throw new IllegalStateException(
                    ENV_AUTH_MODE + "=" + MODE_STRICT
                            + " requiere secretos explicitos no-default: "
                            + ENV_DEMO_CLIENT_SECRET + "/" + ENV_SECRET_CLIENT_SECRET_ID + ", "
                            + ENV_DEMO_TGS_SECRET + "/" + ENV_SECRET_TGS_SECRET_ID + ", "
                            + ENV_DEMO_SERVICE_SECRET + "/" + ENV_SECRET_SERVICE_SECRET_ID);
        }
        if (config.usesPostgresStorage() || config.usesPostgresSessionStorage()) {
            requireConfigured(environment, ENV_POSTGRES_URL, "postgres requiere URL explicita en strict mode");
            requireConfigured(environment, ENV_POSTGRES_USER, "postgres requiere usuario explicito en strict mode");
        }
    }

    private static void requireConfigured(Map<String, String> environment, String key, String message) {
        String value = environment.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message + ": " + key);
        }
    }

    private static boolean isSupportedStorageMode(String configured) {
        return STORAGE_MODE_MEMORY.equals(configured)
                || STORAGE_MODE_SQLITE.equals(configured)
                || STORAGE_MODE_POSTGRES.equals(configured);
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

    private static boolean booleanValue(Map<String, String> environment, String key, boolean defaultValue) {
        String configured = environment.get(key);
        if (configured == null || configured.isBlank()) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(configured)) {
            return true;
        }
        if ("false".equalsIgnoreCase(configured)) {
            return false;
        }
        return defaultValue;
    }
}
