CREATE INDEX IF NOT EXISTS idx_auth_audit_events_client_id
ON auth_audit_events (client_id);

CREATE INDEX IF NOT EXISTS idx_auth_audit_events_service_id
ON auth_audit_events (service_id);

CREATE INDEX IF NOT EXISTS idx_auth_audit_events_event_type
ON auth_audit_events (event_type);
