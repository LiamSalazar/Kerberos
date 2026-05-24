package com.portfolio.auth.core.secrets;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvSecretsProviderTest {
    @Test
    void shouldResolveEnvironmentSecret() {
        EnvSecretsProvider provider = new EnvSecretsProvider(Map.of("AUTH_SECRET", "value-from-env"));

        assertEquals("value-from-env", provider.resolveRequired(SecretRef.env("AUTH_SECRET")));
    }

    @Test
    void shouldFailWhenRequiredSecretIsMissing() {
        EnvSecretsProvider provider = new EnvSecretsProvider(Map.of());

        SecretResolutionException error = assertThrows(
                SecretResolutionException.class,
                () -> provider.resolveRequired(SecretRef.env("AUTH_SECRET")));

        assertTrue(error.getMessage().contains("AUTH_SECRET"));
    }
}
