package com.portfolio.auth.service;

import com.portfolio.auth.core.config.AuthConfig;

import java.util.Objects;

public final class DemoProtectedResource implements ProtectedResource {
    public static final String DEMO_MESSAGE =
            "--------- ACCESO CONCEDIDO A MELODYFINDER --------- MODULAR AUTH EXITOSO ---------";

    private final String serviceId;
    private final String message;

    public DemoProtectedResource(String serviceId, String message) {
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
        this.message = Objects.requireNonNull(message, "message");
    }

    public static DemoProtectedResource create() {
        return new DemoProtectedResource(AuthConfig.DEFAULT_LOCAL_SERVICE_ID, DEMO_MESSAGE);
    }

    public static DemoProtectedResource fromConfig(AuthConfig config) {
        return new DemoProtectedResource(config.defaultServiceId(), DEMO_MESSAGE);
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public ProtectedServiceResponse execute(ProtectedServiceRequest request) {
        return new ProtectedServiceResponse(message, true);
    }
}
