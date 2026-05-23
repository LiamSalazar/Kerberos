CREATE TABLE IF NOT EXISTS auth_audit_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    request_id TEXT NOT NULL,
    client_id TEXT,
    service_id TEXT,
    event_type TEXT NOT NULL,
    status TEXT NOT NULL,
    error_type TEXT,
    latency_ms INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_auth_audit_events_request_id
ON auth_audit_events (request_id);

CREATE INDEX IF NOT EXISTS idx_auth_audit_events_created_at
ON auth_audit_events (created_at);
