CREATE TABLE IF NOT EXISTS auth_sessions (
    session_id TEXT PRIMARY KEY,
    request_id TEXT NOT NULL,
    client_id TEXT NOT NULL,
    service_id TEXT NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status TEXT NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT auth_sessions_revoked_at_check
        CHECK (
            (status = 'ACTIVE' AND revoked_at IS NULL)
            OR (status = 'REVOKED' AND revoked_at IS NOT NULL)
        )
);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_request_id
ON auth_sessions (request_id);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_client_id
ON auth_sessions (client_id);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_service_id
ON auth_sessions (service_id);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_expires_at
ON auth_sessions (expires_at);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_status_expires_at
ON auth_sessions (status, expires_at);
