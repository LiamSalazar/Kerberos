package com.portfolio.auth.client;

import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.dto.ServiceResponse;

public final class AuthFlowRunner {
    private final AuthClient client;

    public AuthFlowRunner(AuthClient client) {
        this.client = client;
    }

    public static AuthFlowRunner localDemo() {
        return new AuthFlowRunner(new AuthClient(AuthConfig.fromEnvironment()));
    }

    public ServiceResponse run() throws Exception {
        return client.runFullFlow();
    }
}
