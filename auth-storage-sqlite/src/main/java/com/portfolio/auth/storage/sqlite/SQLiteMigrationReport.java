package com.portfolio.auth.storage.sqlite;

import java.util.List;

public record SQLiteMigrationReport(
        int appliedCount,
        int skippedCount,
        List<String> appliedVersions
) {
    public SQLiteMigrationReport {
        appliedVersions = List.copyOf(appliedVersions);
    }
}
