package com.portfolio.auth.service;

public final class ProtectedResource {
    private final String message;

    public ProtectedResource(String message) {
        this.message = message;
    }

    public static ProtectedResource demo() {
        return new ProtectedResource(
                "--------- ACCESO CONCEDIDO A MELODYFINDER --------- MODULAR AUTH EXITOSO ---------");
    }

    public String read() {
        return message;
    }
}
