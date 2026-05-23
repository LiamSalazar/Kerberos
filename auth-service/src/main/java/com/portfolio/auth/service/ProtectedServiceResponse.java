package com.portfolio.auth.service;

public record ProtectedServiceResponse(
        String serviceMessage,
        boolean accessGranted
) {
}
