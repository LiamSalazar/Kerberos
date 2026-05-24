package com.portfolio.auth.core.secrets;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class EnvSecretsProvider implements SecretsProvider {
    private final Map<String, String> environment;

    public EnvSecretsProvider() {
        this(System.getenv());
    }

    public EnvSecretsProvider(Map<String, String> environment) {
        this.environment = Map.copyOf(Objects.requireNonNull(environment, "environment"));
    }

    @Override
    public Optional<String> resolve(SecretRef secretRef) {
        Objects.requireNonNull(secretRef, "secretRef");
        if (!"env".equals(secretRef.provider())) {
            return Optional.empty();
        }
        String value = environment.get(secretRef.id());
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }
}
