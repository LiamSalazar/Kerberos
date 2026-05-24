package com.portfolio.auth.core.secrets;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AwsSecretsManagerProviderTest {
    @Test
    void shouldResolveSecretWithInjectedResolver() {
        AwsSecretsManagerProvider provider = new AwsSecretsManagerProvider(
                secretId -> Map.of("client-secret-id", "client-secret-value").get(secretId));

        assertEquals(
                "client-secret-value",
                provider.resolveRequired(SecretRef.awsSecretsManager("client-secret-id")));
    }

    @Test
    void shouldIgnoreNonAwsSecretRefs() {
        AwsSecretsManagerProvider provider = new AwsSecretsManagerProvider(secretId -> "unused");

        assertFalse(provider.resolve(SecretRef.env("AUTH_SECRET")).isPresent());
    }
}
