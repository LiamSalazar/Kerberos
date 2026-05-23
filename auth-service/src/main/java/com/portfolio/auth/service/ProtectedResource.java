package com.portfolio.auth.service;

public interface ProtectedResource {
    String getServiceId();

    ProtectedServiceResponse execute(ProtectedServiceRequest request);

    static ProtectedResource demo() {
        return DemoProtectedResource.create();
    }
}
