package com.portfolio.auth.as;

import com.portfolio.auth.core.config.AuthConfig;

import java.util.Map;
import java.util.Optional;

public final class InMemoryPrincipalRepository {
    private final Map<String, String> clientSecrets;
    private final Map<String, String> ticketGrantingServerSecrets;

    public InMemoryPrincipalRepository(Map<String, String> clientSecrets, Map<String, String> tgsSecrets) {
        this.clientSecrets = Map.copyOf(clientSecrets);
        this.ticketGrantingServerSecrets = Map.copyOf(tgsSecrets);
    }

    public static InMemoryPrincipalRepository fromConfig(AuthConfig config) {
        return new InMemoryPrincipalRepository(
                Map.of(config.defaultClientId(), config.demoClientSecret()),
                Map.of(config.defaultTicketGrantingServerId(), config.demoTicketGrantingServerSecret()));
    }

    public Optional<String> clientSecret(String clientId) {
        return Optional.ofNullable(clientSecrets.get(clientId));
    }

    public Optional<String> ticketGrantingServerSecret(String tgsId) {
        return Optional.ofNullable(ticketGrantingServerSecrets.get(tgsId));
    }
}
