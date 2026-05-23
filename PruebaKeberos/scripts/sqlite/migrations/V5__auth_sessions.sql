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
