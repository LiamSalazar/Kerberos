package com.portfolio.auth.core.audit;

public interface AuditRepository {
    void append(AuthAuditEvent event);
}
