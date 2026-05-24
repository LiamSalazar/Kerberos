package com.portfolio.auth.core.observability;

public record RequestContext(
        String requestId,
        String clientId,
        String serviceId
) {
    public static RequestContext of(String requestId, String clientId, String serviceId) {
        return new RequestContext(requestId, clientId, serviceId);
    }
}
