CREATE TABLE IF NOT EXISTS principals (
    principal_type TEXT NOT NULL,
    id TEXT NOT NULL,
    display_name TEXT NOT NULL,
    secret TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY (principal_type, id)
);

CREATE TABLE IF NOT EXISTS services (
    id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    secret TEXT NOT NULL,
    endpoint TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS schema_version (
    version TEXT PRIMARY KEY,
    description TEXT NOT NULL,
    script_name TEXT NOT NULL,
    checksum TEXT NOT NULL,
    applied_at TEXT NOT NULL
);

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

CREATE INDEX IF NOT EXISTS idx_auth_audit_events_client_id
ON auth_audit_events (client_id);

CREATE INDEX IF NOT EXISTS idx_auth_audit_events_service_id
ON auth_audit_events (service_id);

CREATE INDEX IF NOT EXISTS idx_auth_audit_events_created_at
ON auth_audit_events (created_at);

CREATE TABLE IF NOT EXISTS auth_sessions (
    session_id TEXT PRIMARY KEY,
    request_id TEXT NOT NULL,
    client_id TEXT NOT NULL,
    service_id TEXT NOT NULL,
    issued_at TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    status TEXT NOT NULL,
    revoked_at TEXT,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_request_id
ON auth_sessions (request_id);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_client_id
ON auth_sessions (client_id);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_service_id
ON auth_sessions (service_id);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_expires_at
ON auth_sessions (expires_at);
