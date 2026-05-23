package com.portfolio.auth.client;

import com.portfolio.auth.core.protocol.dto.ErrorResponse;

public final class AuthClientException extends Exception {
    private final ErrorResponse errorResponse;

    public AuthClientException(ErrorResponse errorResponse) {
        super(errorResponse.errorCode() + ": " + errorResponse.errorMessage());
        this.errorResponse = errorResponse;
    }

    public ErrorResponse errorResponse() {
        return errorResponse;
    }
}
