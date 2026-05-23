package com.portfolio.auth.core.session;

public enum SessionValidationStatus {
    VALID,
    NOT_FOUND,
    EXPIRED,
    REVOKED,
    CLIENT_MISMATCH,
    SERVICE_MISMATCH
}
