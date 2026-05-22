package com.portfolio.auth.storage.sqlite;

public record SQLiteServiceRecord(
        String id,
        String displayName,
        String endpoint,
        boolean enabled
) {
}
