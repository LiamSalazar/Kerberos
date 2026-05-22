package com.portfolio.auth.core.audit;

import java.util.List;

public enum NoOpAuthEventRepository implements AuthEventRepository {
    INSTANCE;

    @Override
    public void append(AuthAuditEvent event) {
        // Memory mode keeps audit non-persistent by design.
    }

    @Override
    public List<AuthAuditEvent> findRecent(int limit) {
        return List.of();
    }

    @Override
    public List<AuthAuditEvent> findByRequestId(String requestId) {
        return List.of();
    }
}
