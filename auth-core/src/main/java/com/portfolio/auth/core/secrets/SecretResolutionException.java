package com.portfolio.auth.core.secrets;

public final class SecretResolutionException extends RuntimeException {
    public SecretResolutionException(String message) {
        super(message);
    }

    public SecretResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
