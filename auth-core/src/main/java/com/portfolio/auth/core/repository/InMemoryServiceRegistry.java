package com.portfolio.auth.core.repository;

import com.portfolio.auth.core.config.AuthConfig;

import java.util.Map;
import java.util.Optional;

public final class InMemoryServiceRegistry implements ServiceRegistry {
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

    public static InMemoryServiceRegistry forServiceSecrets(Map<String, String> serviceSecrets) {
        return new InMemoryServiceRegistry(Map.of(), serviceSecrets);
    }

    @Override
    public Optional<String> ticketGrantingServerSecret(String tgsId) {
        return Optional.ofNullable(ticketGrantingServerSecrets.get(tgsId));
    }

    @Override
    public Optional<String> serviceSecret(String serviceId) {
        return Optional.ofNullable(serviceSecrets.get(serviceId));
    }
}
