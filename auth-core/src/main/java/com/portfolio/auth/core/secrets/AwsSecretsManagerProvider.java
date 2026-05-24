package com.portfolio.auth.core.secrets;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class AwsSecretsManagerProvider implements SecretsProvider, AutoCloseable {
    private final Function<String, String> secretResolver;
    private final AutoCloseable closeable;

    public AwsSecretsManagerProvider(String region) {
        this(client(region));
    }

    public AwsSecretsManagerProvider(SecretsManagerClient client) {
        Objects.requireNonNull(client, "client");
        this.secretResolver = secretId -> {
            try {
                return client.getSecretValue(GetSecretValueRequest.builder()
                                .secretId(secretId)
                                .build())
                        .secretString();
            } catch (SecretsManagerException exception) {
                throw new SecretResolutionException(
                        "Could not resolve AWS Secrets Manager secret id=" + secretId,
                        exception);
            }
        };
        this.closeable = client;
    }

    public AwsSecretsManagerProvider(Function<String, String> secretResolver) {
        this.secretResolver = Objects.requireNonNull(secretResolver, "secretResolver");
        this.closeable = null;
    }

    @Override
    public Optional<String> resolve(SecretRef secretRef) {
        Objects.requireNonNull(secretRef, "secretRef");
        if (!"aws-secrets-manager".equals(secretRef.provider())) {
            return Optional.empty();
        }
        String value = secretResolver.apply(secretRef.id());
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    @Override
    public void close() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    private static SecretsManagerClient client(String region) {
        String normalized = Objects.requireNonNull(region, "region").trim();
        if (normalized.isBlank()) {
            throw new SecretResolutionException("AUTH_AWS_REGION is required for AWS Secrets Manager");
        }
        return SecretsManagerClient.builder()
                .region(Region.of(normalized))
                .build();
    }
}
