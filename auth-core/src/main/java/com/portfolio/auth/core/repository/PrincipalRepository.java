package com.portfolio.auth.core.repository;

import java.util.Optional;

public interface PrincipalRepository {
    Optional<String> clientSecret(String clientId);

    Optional<String> ticketGrantingServerSecret(String tgsId);
}
