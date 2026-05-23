package com.portfolio.auth.storage.sqlite;

public record SQLiteClientRecord(
        String id,
        String displayName,
        boolean enabled
) {
}
