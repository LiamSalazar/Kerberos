package com.portfolio.auth.core.repository;

import java.util.Optional;

public interface ServiceRegistry {
    Optional<String> ticketGrantingServerSecret(String tgsId);

    Optional<String> serviceSecret(String serviceId);
}
