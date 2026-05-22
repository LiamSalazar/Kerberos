package com.portfolio.auth.service;

public final class ProtectedResource {
    private final String message;

    public ProtectedResource(String message) {
        this.message = message;
    }

    public static ProtectedResource demo() {
        return new ProtectedResource(
                "This is a protected resource. You have successfully authenticated using Kerberos and can access this message.");
    }

    public String read() {
        return message;
    }
}
