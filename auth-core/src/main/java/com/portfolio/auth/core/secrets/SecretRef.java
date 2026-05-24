package com.portfolio.auth.core.secrets;

import java.util.Objects;

public record SecretRef(
        String provider,
        String id
) {
    public SecretRef {
        provider = requireText(provider, "provider");
        id = requireText(id, "id");
    }

    public static SecretRef env(String variableName) {
        return new SecretRef("env", variableName);
    }

    public static SecretRef awsSecretsManager(String secretId) {
        return new SecretRef("aws-secrets-manager", secretId);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
