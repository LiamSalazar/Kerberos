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

CREATE TABLE IF NOT EXISTS auth_audit_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    request_id TEXT NOT NULL,
    client_id TEXT,
    service_id TEXT,
    event_type TEXT NOT NULL,
    created_at TEXT NOT NULL,
    status TEXT NOT NULL
);
