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
