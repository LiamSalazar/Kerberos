package com.portfolio.auth.core.audit;

import java.util.List;

public interface AuthEventRepository extends AuditRepository {
    List<AuthAuditEvent> findRecent(int limit);

    List<AuthAuditEvent> findByRequestId(String requestId);

    List<AuthAuditEvent> findByClientId(String clientId);

    List<AuthAuditEvent> findByServiceId(String serviceId);
}
