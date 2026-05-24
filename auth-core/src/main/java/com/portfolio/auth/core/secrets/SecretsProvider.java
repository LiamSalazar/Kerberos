package com.portfolio.auth.core.secrets;

import java.util.Optional;

public interface SecretsProvider {
    Optional<String> resolve(SecretRef secretRef);

    default String resolveRequired(SecretRef secretRef) {
        return resolve(secretRef)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new SecretResolutionException(
                        "Required secret is missing for provider=" + secretRef.provider()
                                + ", id=" + secretRef.id()));
    }
}
