package com.portfolio.auth.tgs;

import com.portfolio.auth.core.config.AuthConfig;

import java.util.Map;
import java.util.Optional;

public final class InMemoryServiceRegistry {
    private final Map<String, String> ticketGrantingServerSecrets;
    private final Map<String, String> serviceSecrets;

    public InMemoryServiceRegistry(Map<String, String> tgsSecrets, Map<String, String> serviceSecrets) {
        this.ticketGrantingServerSecrets = Map.copyOf(tgsSecrets);
        this.serviceSecrets = Map.copyOf(serviceSecrets);
    }

    public static InMemoryServiceRegistry fromConfig(AuthConfig config) {
        return new InMemoryServiceRegistry(
                Map.of(config.defaultTicketGrantingServerId(), config.demoTicketGrantingServerSecret()),
                Map.of(config.defaultServiceId(), config.demoServiceSecret()));
    }

    public Optional<String> ticketGrantingServerSecret(String tgsId) {
        return Optional.ofNullable(ticketGrantingServerSecrets.get(tgsId));
    }

    public Optional<String> serviceSecret(String serviceId) {
        return Optional.ofNullable(serviceSecrets.get(serviceId));
    }
}
